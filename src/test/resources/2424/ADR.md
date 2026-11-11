# ADR-001: Централизация контекста запроса и обработки интеграционных ошибок

**Статус:** Proposed

### Контекст

В существующей реализации контроллеры вручную записывают данные в `HttpServletRequest` через `setAttributes()`.

Эти атрибуты используются `EasupIntegrationExceptionHandler` для формирования ошибок, логирования и отправки событий аудита.

Это создаёт зависимость обработки ошибок от выполнения конкретного метода контроллера.

### Решение

1. Сохранить существующую реализацию без изменений.
2. Новые и переносимые эндпоинты реализовывать с использованием `@SapSystemParams`.
3. Централизовать формирование контекста запроса.
4. Использовать типизированные исключения для интеграционных ошибок.
5. Создать отдельный `@RestControllerAdvice` для новой реализации.
6. Отделить отправку событий аудита от формирования HTTP-ответов.

### Ограничения

- Не менять поведение существующих эндпоинтов.
- Сохранить согласованный HTTP-контракт переносимых эндпоинтов.
- Не потерять обязательные события аудита.
- Внедрять изменения поэтапно с тестированием.

---

## Состав

- `annotation/RequestType.java` — идентификатор операции на методе.
- `annotation/MigratedEasupEndpoint.java` — маркер новых контроллеров; изоляция нового `ControllerAdvice`.
- `controller/PayavgController.java` — REST-эндпоинт.
- `service/PayavgService.java` — правило `null → 404`.
- `port/IntegrationErrorMapper.java` — готовый преобразователь ошибок для RestTemplate-подобного клиента, опционально.
- `exception/*` — типизированные исключения.
- `audit/*` — стратегии, resolver, dispatcher и параметры аудита.
- `handler/IntegrationExceptionHandler.java` — HTTP-ответы и вызов аудита.
- `src/test` — unit-тесты resolver и сервиса.


### Рассматриваемые варианты

Что имеем сейчас
```java

@GetMapping(value = "/event", produces = "application/json")
public ResponseEntity<Object> getPayavgEvent(
        HttpServletRequest request
) throws Exception {

    SapSystemParamsDto systemParams = setSystemParams(request);
    setAttributes(request, "easup_payavg_event");

    PayavgEventDto dto =
            easupPaystubPayavgIntegrationService
                    .getPayavgEvent(systemParams);

    log.info("Успешное выполнение эндпоинта [SRHR_EASUP_REQUEST_GET_PAYAVG_EVENT]");

    if (dto != null) {
        return new ResponseEntity<>(dto, HttpStatus.OK);
    } else {
        log.warn("Неуспешное выполнение эндпоинта: вернулось нулевое значение");
        throw new HttpResponseException(
                404,
                ErrorResponse.EASUP_REQUEST_NOT_FOUND
        );
    }
}
```

Основные проблемы:
1. Контроллер вручную создаёт SapSystemParamsDto.
2. Контроллер вручную подготавливает данные для ControllerAdvice.
3. Контроллер самостоятельно определяет, когда выбросить HTTP-исключение.
4. Используется ResponseEntity<Object> вместо конкретного типа.

Как предлагаю сделать

Создаём `@RequestType`

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestType {
    String value();
}
```

Кнтроллер будет такой

```java
@RequestType("easup_payavg_event")
@GetMapping(value = "/event", produces = "application/json")
public PayavgEventDto getPayavgEvent(
        @SapSystemParams SapSystemParamsDto params
) {
    return payavgService.getPayavgEvent(params);
}
```

Можем ли перестать перекладывать данные из HTTP-заголовков в атрибуты запроса?

```java
@Slf4j
@RestControllerAdvice(annotations = MigratedEasupEndpoint.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class IntegrationExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ServiceErrorDto> unexpected(
            Exception ex, HttpServletRequest request) {
        auditDispatcher.sendSafely(request, AuditErrorType.UNEXPECTED);
        log.error("EASUP operation {}", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Внутренняя ошибка сервиса", request);
    }

    private ResponseEntity<ServiceErrorDto> build(
            int status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(error(message, String.valueOf(status), request));
    }

    private ServiceErrorDto error(
            String message, String code, HttpServletRequest request) {
        String traceId = request.getHeader("logId");
        if (traceId == null || traceId.isBlank()) {
            traceId = "undefined";
        }
        return new ServiceErrorDto(new ServiceError(
                message, code, traceId, new ArrayList<>()));
    }
    
}    
```

В сервисе выкидывать исключения, и обрабатывать их в RestControllerAdvice
