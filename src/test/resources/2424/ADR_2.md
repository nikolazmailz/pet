# ADR-001: Централизация контекста запроса и обработки интеграционных ошибок

**Статус:** Proposed

### Контекст

В существующей реализации контроллеры вручную записывают данные в `HttpServletRequest` через `setAttributes()`.

Эти атрибуты используются `EasupIntegrationExceptionHandler` для формирования ошибок, логирования и отправки событий аудита.

Это создаёт зависимость обработки ошибок от выполнения конкретного метода контроллера.

### Основные изменения
- `Controller` — исключаем setSystemParams(), setAttributes() и обработку ошибок.
- `SapSystemParamsDto` — сохраняем существующий механизм @SapSystemParams.
- `@RequestType` — идентифицируем операцию через аннотацию.
- `Service` — используем типизированные исключения.
- `ControllerAdvice` — централизуем формирование ServiceErrorDto.
- `@MigratedEasupEndpoint` — ограничиваем применение нового обработчика перенесёнными контроллерами.

### Ограничения
- Существующая реализация остаётся без изменений.
- Сохраняется согласованный HTTP-контракт.
- Новый обработчик применяется только к контроллерам с @MigratedEasupEndpoint.
- Корректность работы двух ControllerAdvice проверяется интеграционными тестами.

### Результат

Упрощение контроллеров, устранение дублирования и централизованная 
обработка интеграционных ошибок без изменения существующего подхода к передаче SapSystemParamsDto.

---

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

### Предлагаемое решение

Упростить контроллер

```java
@RestController
@MigratedEasupEndpoint
@RequiredArgsConstructor
public class PayavgController {

    private final PayavgService service;

    @RequestType("easup_payavg_event")
    @GetMapping("/event")
    public PayavgEventDto getPayavgEvent(
            @SapSystemParams SapSystemParamsDto params) {

        return service.getPayavgEvent(params);
    }
}
```

Перенести проверку результата в сервис

```java
public PayavgEventDto getPayavgEvent(SapSystemParamsDto params) {

    PayavgEventDto result = integrationService.getPayavgEvent(params);

    if (result == null) {
        throw new PayavgEventNotFoundException();
    }

    return result;
}
```

Создать отдельный ControllerAdvice

```java
@Slf4j
@RestControllerAdvice(annotations = MigratedEasupEndpoint.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IntegrationExceptionHandler {

    @ExceptionHandler(PayavgEventNotFoundException.class)
    public ResponseEntity<ServiceErrorDto> notFound(
            PayavgEventNotFoundException ex,
            HttpServletRequest request) {

        return build(404, ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ServiceErrorDto> unexpected(
            Exception ex, HttpServletRequest request) {

        log.error("EASUP operation failed", ex);

        return build(500, "Внутренняя ошибка сервиса", request);
    }

    private ResponseEntity<ServiceErrorDto> build(
            int status, String message, HttpServletRequest request) {

        String traceId = request.getHeader("logId");

        if (traceId == null || traceId.isBlank()) {
            traceId = "undefined";
        }

        ServiceErrorDto error = new ServiceErrorDto(
                new ServiceError(
                        message,
                        String.valueOf(status),
                        traceId,
                        new ArrayList<>()
                )
        );

        return ResponseEntity.status(status).body(error);
    }
}
```
