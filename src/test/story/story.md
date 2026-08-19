## Servlet filter
Изменить SapSystemParamsDto в контроллерах
Реализовать при помощи фильтра

```java

@Component
public class SapRequestContextFilter extends OncePerRequestFilter {

    public static final String SAP_REQUEST_CONTEXT =
            SapRequestContext.class.getName();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        SapRequestContext context = new SapRequestContext(
                getRequiredAttribute(request, "channel"),
                getRequiredAttribute(request, "adLogin"),
                getRequiredAttribute(request, "sessionId"),
                getRequiredAttribute(request, "realm")
        );

        request.setAttribute(SAP_REQUEST_CONTEXT, context);

        filterChain.doFilter(request, response);
    }

    private String getRequiredAttribute(
            HttpServletRequest request,
            String name
    ) {
        Object value = request.getAttribute(name);

        if (value == null) {
            throw new IllegalStateException(
                    "Отсутствует атрибут запроса: " + name
            );
        }

        return value.toString();
    }
}

public record SapRequestContext(
        String channel,
        String adLogin,
        String sessionId,
        String realm
) {
}
```


Контроллер перегружен технической логикой

Сейчас он:

устанавливает request attribute;
собирает системные параметры;
вызывает сервис;
логирует интеграционный ответ;
интерпретирует код внешней системы;
маппит DTO;
очищает DTO;
формирует HTTP-ошибку.

Контроллер в идеале должен выглядеть приблизительно так:

```java
@PostMapping
public ResponseEntity<RequisitionGeneralCandidateDtoOut> addComment(
        @Valid @RequestBody RequisitionAddCommentRequest requestDto,
        SapSystemParamsDto systemParams
) {
    RequisitionGeneralCandidateDtoOut response =
            requisitionService.addComment(systemParams, requestDto);

    return ResponseEntity.ok(response);
}
```

# Рефакторинг контроллеров: передача системных параметров и обработка ответов

## Задача 1. Централизованное получение системных параметров запроса

### Название

**Вынести получение `SapSystemParamsDto` в `HandlerMethodArgumentResolver`**

### Цель

Исключить дублирование логики извлечения системных параметров из `HttpServletRequest` в контроллерах и предоставить единый механизм получения готового объекта `SapSystemParamsDto`.

### Текущее состояние

В контроллерах системные параметры формируются вручную:

```java
private SapSystemParamsDto setSystemParams(HttpServletRequest request) {
    SapSystemParamsDto systemParams = new SapSystemParamsDto();
    systemParams.setChannel(request.getAttribute("channel").toString());
    systemParams.setAdLogin(request.getAttribute("adLogin").toString());
    systemParams.setSessionId(request.getAttribute("sessionId").toString());
    systemParams.setRealm(request.getAttribute("realm").toString());
    return systemParams;
}
```

Недостатки текущего решения:

* логика дублируется между контроллерами;
* контроллеры напрямую зависят от `HttpServletRequest`;
* отсутствует единая проверка обязательных параметров;
* возможен `NullPointerException` при отсутствии атрибута;
* усложняется тестирование контроллеров.

### Требуемые изменения

1. Создать аннотацию для параметра контроллера:

```java
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SapSystemParams {
}
```

2. Реализовать `HandlerMethodArgumentResolver`, который:

* поддерживает параметры типа `SapSystemParamsDto`, отмеченные аннотацией `@SapSystemParams`;
* получает необходимые значения из атрибутов текущего запроса;
* проверяет наличие обязательных параметров;
* формирует `SapSystemParamsDto`;
* выбрасывает специализированное исключение при отсутствии обязательного параметра.

Пример использования:

```java
public ResponseEntity<RequisitionGeneralCandidateDtoOut> postRequisitionAddComment(
        @RequestBody RequisitionAddCommentRequest requestDto,
        @SapSystemParams SapSystemParamsDto systemParams
) {
    return ResponseEntity.ok(
            requisitionService.addComment(systemParams, requestDto)
    );
}
```

3. Зарегистрировать resolver через `WebMvcConfigurer`.

4. Вынести в общий модуль `commons`:

* аннотацию `@SapSystemParams`;
* `HandlerMethodArgumentResolver`;
* исключение отсутствия системного параметра;
* константы с именами request-атрибутов;
* конфигурацию регистрации resolver.

5. Удалить из контроллеров методы вида:

```java
setSystemParams(HttpServletRequest request)
```

6. Передавать полученный `SapSystemParamsDto` явно дальше:

```text
Controller → Service → Integration adapter
```

Не использовать статический holder или собственный `ThreadLocal`.

### Пример реализации resolver

```java
@Component
public class SapSystemParamsArgumentResolver
        implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(SapSystemParams.class)
                && SapSystemParamsDto.class.isAssignableFrom(
                        parameter.getParameterType()
                );
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        HttpServletRequest request =
                webRequest.getNativeRequest(HttpServletRequest.class);

        if (request == null) {
            throw new SapSystemParamsException(
                    "Не удалось получить текущий HTTP-запрос"
            );
        }

        SapSystemParamsDto result = new SapSystemParamsDto();
        result.setChannel(requiredAttribute(request, "channel"));
        result.setAdLogin(requiredAttribute(request, "adLogin"));
        result.setSessionId(requiredAttribute(request, "sessionId"));
        result.setRealm(requiredAttribute(request, "realm"));

        return result;
    }

    private String requiredAttribute(
            HttpServletRequest request,
            String attributeName
    ) {
        Object value = request.getAttribute(attributeName);

        if (value == null || value.toString().isBlank()) {
            throw new SapSystemParamsException(
                    "Отсутствует обязательный параметр запроса: "
                            + attributeName
            );
        }

        return value.toString();
    }
}
```

### Критерии приёмки

* В контроллерах отсутствует ручное создание `SapSystemParamsDto`.
* Контроллеры не используют `HttpServletRequest` только ради получения системных параметров.
* `SapSystemParamsDto` доступен как аргумент метода контроллера.
* При отсутствии обязательного параметра выбрасывается специализированное исключение.
* Компоненты resolver размещены в модуле `commons`.
* `SapSystemParamsDto` передаётся явно в сервисный и интеграционный слои.
* Добавлены unit-тесты для resolver.
* Добавлен интеграционный тест контроллера с корректными параметрами.
* Добавлен тест запроса с отсутствующим обязательным параметром.

---

## Задача 2. Типизация успешных ответов контроллеров

### Название

**Заменить `ResponseEntity<Object>` на конкретные DTO ответов**

### Цель

Сделать HTTP-контракты контроллеров явными и типобезопасными, улучшить читаемость кода и корректность генерируемой OpenAPI-документации.

### Текущее состояние

Контроллер возвращает общий тип:

```java
public ResponseEntity<Object> postRequisitionAddComment(...)
```

При успешном выполнении фактически возвращается:

```java
RequisitionGeneralCandidateDtoOut
```

Ошибочные ответы формируются через исключения.

### Проблемы

* из сигнатуры метода неясно, какой объект возвращается;
* ухудшается статическая проверка типов;
* OpenAPI может формировать слишком общий контракт;
* клиентам API сложнее определить структуру успешного ответа;
* контроллер потенциально может вернуть произвольный объект.

### Требуемые изменения

1. Заменить:

```java
ResponseEntity<Object>
```

на:

```java
ResponseEntity<RequisitionGeneralCandidateDtoOut>
```

2. Не возвращать DTO ошибки непосредственно из метода контроллера.

3. Ошибочные ответы формировать централизованно через `ControllerAdvice`.

4. Проверить остальные контроллеры и заменить `ResponseEntity<Object>` там, где успешный тип ответа известен.

5. Для эндпоинтов без тела использовать:

```java
ResponseEntity<Void>
```

6. Для коллекций использовать конкретный тип:

```java
ResponseEntity<List<RequisitionDto>>
```

### Целевой пример

```java
@PostMapping("/requisitions/comments")
public ResponseEntity<RequisitionGeneralCandidateDtoOut>
postRequisitionAddComment(
        @Valid
        @RequestBody RequisitionAddCommentRequest requestDto,
        @SapSystemParams SapSystemParamsDto systemParams
) {
    RequisitionGeneralCandidateDtoOut response =
            requisitionService.addComment(systemParams, requestDto);

    return ResponseEntity.ok(response);
}
```

### Дополнительные требования

Если внешний сервис может вернуть разные успешные статусы, необходимо определить контракт эндпоинта:

* всегда возвращать `200 OK`;
* либо сохранять исходный HTTP-статус внешней системы.

Решение должно быть единообразным для аналогичных эндпоинтов.

### Критерии приёмки

* Метод `postRequisitionAddComment` возвращает конкретный DTO.
* В сигнатуре отсутствует `ResponseEntity<Object>`.
* Ошибочные ответы не входят в generic-параметр `ResponseEntity`.
* OpenAPI содержит корректную схему успешного ответа.
* Существующие успешные сценарии не изменили структуру ответа.
* Добавлен или обновлён тест успешного ответа контроллера.
* Проведён поиск аналогичных методов с `ResponseEntity<Object>`.
* Для найденных методов созданы связанные задачи либо выполнен аналогичный рефакторинг.

---

## Задача 3. Централизованная обработка исключений

### Название

**Вынести формирование HTTP-ошибок в `@RestControllerAdvice`**

### Цель

Убрать из контроллеров повторяющуюся логику преобразования исключений в HTTP-ответы и обеспечить единый формат ошибок API.

### Текущее состояние

Контроллер самостоятельно анализирует ответ внешней системы и выбрасывает исключения:

```java
int httpStatusNumber =
        Integer.parseInt(
                requisitionResponse
                        .getResponseCode()
                        .substring(0, 3)
        );

throw new HttpResponseException(
        httpStatusNumber,
        requisitionResponse.getErrorMessage()
);
```

При отсутствии ответа используется:

```java
throw new HttpResponseException(
        404,
        ErrorResponse.REQUISITION_INFO_NOT_FOUND
);
```

### Проблемы

* логика ошибок распределена по контроллерам;
* отсутствует единый формат ответа;
* контроллеры перегружены техническими проверками;
* возможны необработанные `NullPointerException`,
  `NumberFormatException` и `IndexOutOfBoundsException`;
* одинаковые исключения могут по-разному обрабатываться в разных контроллерах;
* техническая ошибка отсутствия ответа может ошибочно преобразовываться в `404`.

### Требуемые изменения

1. Создать или доработать глобальный обработчик:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
}
```

2. Определить единый DTO ошибки:

```java
public record ApiErrorResponse(
        int status,
        String code,
        String message,
        String path,
        String correlationId,
        LocalDateTime timestamp
) {
}
```

3. Добавить обработчики как минимум для:

* ошибок внешней системы;
* отсутствия ответа внешней системы;
* некорректного ответа внешней системы;
* отсутствия системных параметров;
* ошибок валидации входного запроса;
* неизвестных исключений.

4. Использовать специализированные исключения вместо общего `HttpResponseException`, где это возможно:

```java
ExternalSystemException
ExternalSystemNoResponseException
InvalidExternalResponseException
SapSystemParamsException
ResourceNotFoundException
```

5. В исключении внешней системы хранить HTTP-статус типизированно:

```java
public class ExternalSystemException extends RuntimeException {

    private final HttpStatusCode status;
    private final String errorCode;

    public ExternalSystemException(
            HttpStatusCode status,
            String errorCode,
            String message
    ) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatusCode getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
```

6. Пример обработчика:

```java
@ExceptionHandler(ExternalSystemException.class)
public ResponseEntity<ApiErrorResponse> handleExternalSystemException(
        ExternalSystemException exception,
        HttpServletRequest request
) {
    ApiErrorResponse response = new ApiErrorResponse(
            exception.getStatus().value(),
            exception.getErrorCode(),
            exception.getMessage(),
            request.getRequestURI(),
            MDC.get("uuid"),
            LocalDateTime.now()
    );

    return ResponseEntity
            .status(exception.getStatus())
            .body(response);
}
```

7. Добавить fallback-обработчик:

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
        Exception exception,
        HttpServletRequest request
) {
    log.error("Непредвиденная ошибка обработки запроса", exception);

    ApiErrorResponse response = new ApiErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "INTERNAL_SERVER_ERROR",
            "Произошла внутренняя ошибка сервиса",
            request.getRequestURI(),
            MDC.get("uuid"),
            LocalDateTime.now()
    );

    return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(response);
}
```

Во внешнем ответе не должны раскрываться stack trace, имена Java-классов и технические детали.

### Рекомендуемое соответствие ошибок

| Ситуация                                    |                                                         HTTP-статус |
| ------------------------------------------- | ------------------------------------------------------------------: |
| Запрашиваемый бизнес-ресурс отсутствует     |                                                     `404 Not Found` |
| Некорректный входной запрос                 |                                                   `400 Bad Request` |
| Отсутствует обязательный системный параметр | `400 Bad Request` или `401 Unauthorized` в зависимости от параметра |
| Пользователь не аутентифицирован            |                                                  `401 Unauthorized` |
| Недостаточно прав                           |                                                     `403 Forbidden` |
| Внешняя система вернула ошибку              |                  В соответствии с контрактом либо `502 Bad Gateway` |
| Внешняя система не ответила                 |                                               `504 Gateway Timeout` |
| Внешняя система вернула некорректный ответ  |                                                   `502 Bad Gateway` |
| Неизвестная ошибка приложения               |                                         `500 Internal Server Error` |

### Целевой контроллер

После выполнения всех трёх задач контроллер должен содержать только orchestration-логику:

```java
@PostMapping("/requisitions/comments")
public ResponseEntity<RequisitionGeneralCandidateDtoOut>
postRequisitionAddComment(
        @Valid
        @RequestBody RequisitionAddCommentRequest requestDto,
        @SapSystemParams SapSystemParamsDto systemParams
) {
    RequisitionGeneralCandidateDtoOut response =
            requisitionService.addComment(systemParams, requestDto);

    return ResponseEntity.ok(response);
}
```

Контроллер не должен:

* формировать `SapSystemParamsDto`;
* обращаться к `HttpServletRequest` ради системных параметров;
* разбирать строковый код ответа внешней системы;
* создавать DTO ошибки;
* выбирать текст технической ошибки;
* копировать свойства между интеграционными и API DTO.

### Критерии приёмки

* Создан глобальный `@RestControllerAdvice`.
* Все ошибки API возвращаются в едином формате.
* Контроллер не содержит логики построения ошибочного HTTP-ответа.
* Для ожидаемых ошибок используются специализированные исключения.
* Добавлены тесты каждого обработчика исключений.
* Добавлен тест fallback-обработчика.
* В ответах отсутствуют stack trace и внутренние технические детали.
* HTTP-статусы соответствуют семантике ошибок.
* Логирование исключений выполняется централизованно и без дублирования.

---

# Порядок выполнения

1. Реализовать `HandlerMethodArgumentResolver`.
2. Перевести выбранный контроллер на получение `SapSystemParamsDto` через аргумент метода.
3. Добавить глобальный `ControllerAdvice`.
4. Перенести обработку ошибок из контроллера в сервисный слой и `ControllerAdvice`.
5. Типизировать `ResponseEntity`.
6. Обновить OpenAPI и автоматические тесты.

# Общий результат

После рефакторинга контроллеры должны:

* принимать готовые системные параметры;
* передавать их в сервис явно;
* возвращать конкретный DTO успешного ответа;
* не содержать логики обработки исключений;
* не зависеть от деталей ответа внешней системы;
* иметь единообразный и документированный HTTP-контракт.


