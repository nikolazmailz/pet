## Задача 3. Внедрить иерархию доменных исключений и централизованную обработку ошибок

### Название

**Реализовать иерархию `DomainException` и глобальный `@RestControllerAdvice`**

### Цель

Унифицировать обработку ожидаемых ошибок приложения:

* убрать формирование ошибочных HTTP-ответов из контроллеров;
* отказаться от универсального `HttpResponseException`;
* ввести базовое доменное исключение;
* обеспечить единый формат ответа об ошибке;
* централизовать логирование ожидаемых и непредвиденных исключений.

### Текущее состояние

В контроллерах самостоятельно определяется HTTP-статус и выбрасывается общее исключение:

```java
int httpStatusNumber = Integer.parseInt(
        requisitionResponse.getResponseCode().substring(0, 3)
);

throw new HttpResponseException(
        httpStatusNumber,
        requisitionResponse.getErrorMessage()
);
```

Недостатки текущего подхода:

* контроллер знает особенности ответа внешней системы;
* используется общее исключение без выраженной семантики;
* одинаковые ошибки могут обрабатываться по-разному;
* логирование распределено по контроллерам;
* сложно определить тип ошибки по классу исключения;
* отсутствует единая модель ошибок приложения.

---

### Требуемое решение

Создать базовое исключение `DomainException`, от которого наследуются ожидаемые исключения приложения.

Базовое исключение должно содержать:

* HTTP-статус;
* сообщение для клиента;
* `traceId`;
* при необходимости код ошибки.

```java
@Getter
public abstract class DomainException extends RuntimeException {

    private final HttpStatus status;
    private final String traceId;

    protected DomainException(
            HttpStatus status,
            String message,
            String traceId
    ) {
        super(message);
        this.status = status;
        this.traceId = traceId;
    }

    protected DomainException(
            HttpStatus status,
            String message,
            String traceId,
            Throwable cause
    ) {
        super(message, cause);
        this.status = status;
        this.traceId = traceId;
    }
}
```

`DomainException` должно использоваться только для ожидаемых ошибок, которые приложение умеет преобразовать в определённый HTTP-ответ.

---

### Иерархия исключений

Рекомендуемая структура:

```text
DomainException
├── BusinessException
│   ├── RequisitionNotFoundException
│   ├── InvalidRequisitionStateException
│   └── OperationNotAllowedException
│
├── RequestContextException
│   └── RequiredSystemParameterMissingException
│
└── ExternalSystemException
    ├── ExternalSystemResponseException
    ├── ExternalSystemUnavailableException
    ├── ExternalSystemTimeoutException
    └── InvalidExternalSystemResponseException
```

Необязательно создавать все классы сразу. На первом этапе необходимо реализовать только исключения, которые используются в рефакторируемом сценарии.

---

### Бизнес-исключения

Бизнес-исключения описывают ожидаемые нарушения правил приложения.

```java
public abstract class BusinessException extends DomainException {

    protected BusinessException(
            HttpStatus status,
            String message,
            String traceId
    ) {
        super(status, message, traceId);
    }
}
```

Пример исключения отсутствия заявки:

```java
public class RequisitionNotFoundException extends BusinessException {

    public RequisitionNotFoundException(String traceId) {
        super(
                HttpStatus.NOT_FOUND,
                ErrorResponse.REQUISITION_INFO_NOT_FOUND,
                traceId
        );
    }
}
```

Пример нарушения состояния:

```java
public class InvalidRequisitionStateException extends BusinessException {

    public InvalidRequisitionStateException(
            String message,
            String traceId
    ) {
        super(
                HttpStatus.CONFLICT,
                message,
                traceId
        );
    }
}
```

---

### Исключения контекста запроса

Ошибки получения обязательных системных параметров должны также входить в общую иерархию.

```java
public class RequiredSystemParameterMissingException
        extends DomainException {

    public RequiredSystemParameterMissingException(
            String parameterName,
            String traceId
    ) {
        super(
                HttpStatus.BAD_REQUEST,
                "Отсутствует обязательный системный параметр: "
                        + parameterName,
                traceId
        );
    }
}
```

Это исключение должно выбрасываться из `HandlerMethodArgumentResolver`, если обязательный атрибут отсутствует или имеет пустое значение.

---

### Интеграционные исключения

Ошибки взаимодействия с ЕАСУП или другой внешней системой должны быть выражены отдельными типами исключений.

```java
public abstract class ExternalSystemException
        extends DomainException {

    protected ExternalSystemException(
            HttpStatus status,
            String message,
            String traceId
    ) {
        super(status, message, traceId);
    }

    protected ExternalSystemException(
            HttpStatus status,
            String message,
            String traceId,
            Throwable cause
    ) {
        super(status, message, traceId, cause);
    }
}
```

Отсутствие ответа:

```java
public class ExternalSystemUnavailableException
        extends ExternalSystemException {

    public ExternalSystemUnavailableException(
            String message,
            String traceId
    ) {
        super(
                HttpStatus.BAD_GATEWAY,
                message,
                traceId
        );
    }
}
```

Таймаут:

```java
public class ExternalSystemTimeoutException
        extends ExternalSystemException {

    public ExternalSystemTimeoutException(
            String message,
            String traceId
    ) {
        super(
                HttpStatus.GATEWAY_TIMEOUT,
                message,
                traceId
        );
    }
}
```

Некорректный ответ:

```java
public class InvalidExternalSystemResponseException
        extends ExternalSystemException {

    public InvalidExternalSystemResponseException(
            String message,
            String traceId
    ) {
        super(
                HttpStatus.BAD_GATEWAY,
                message,
                traceId
        );
    }
}
```

Ошибка, возвращённая внешней системой:

```java
public class ExternalSystemResponseException
        extends ExternalSystemException {

    public ExternalSystemResponseException(
            HttpStatus status,
            String message,
            String traceId
    ) {
        super(status, message, traceId);
    }
}
```

Решение о проксировании статуса внешней системы должно соответствовать контракту API.

Не следует автоматически возвращать клиенту любой статус внешней системы без проверки. Например, технические ошибки внешнего сервиса могут преобразовываться в `502 Bad Gateway`.

---

### Модель ошибки API

Все ожидаемые ошибки должны возвращаться в едином формате:

```java
public record ApiError(
        String message,
        String traceId
) {

    public static ApiError of(
            String message,
            String traceId
    ) {
        return new ApiError(message, traceId);
    }
}
```

При необходимости модель может быть расширена:

```java
public record ApiError(
        String code,
        String message,
        String traceId,
        LocalDateTime timestamp
) {
}
```

Но изменение существующего контракта ответа необходимо выполнять отдельно, если оно влияет на клиентов API.

---

### Глобальный обработчик исключений

Создать единый обработчик:

```java
@Slf4j
@RestControllerAdvice
public class DomainExceptionHandler
        extends ResponseEntityExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiError> handleDomainException(
            DomainException ex
    ) {
        HttpStatus status = ex.getStatus();

        if (status.is5xxServerError()) {
            log.error(
                    "Ошибка обработки запроса [{}]: {}",
                    ex.getTraceId(),
                    ex.getMessage(),
                    ex
            );
        } else {
            log.warn(
                    "Неуспешное выполнение запроса [{}]: {}",
                    ex.getTraceId(),
                    ex.getMessage()
            );
        }

        return ResponseEntity
                .status(status)
                .body(
                        ApiError.of(
                                ex.getMessage(),
                                ex.getTraceId()
                        )
                );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(
            Exception ex
    ) {
        log.error("Непредвиденная ошибка", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        ApiError.of(
                                ErrorResponse.INTERNAL_ERROR,
                                null
                        )
                );
    }
}
```

Название метода рекомендуется изменить с:

```java
handleSrhr(...)
```

на:

```java
handleDomainException(...)
```

Поскольку обработчик работает со всей иерархией `DomainException`, а не с одним конкретным типом ошибки.

---

### Получение `traceId`

Желательно использовать единый источник `traceId`.

Если `traceId` уже содержится в ответе внешней системы, он должен передаваться в соответствующее исключение:

```java
throw new ExternalSystemResponseException(
        status,
        requisitionResponse.getErrorMessage(),
        requisitionResponse.getTraceId()
);
```

Если исключение возникло внутри приложения, `traceId` можно получать из MDC:

```java
String traceId = MDC.get("traceId");
```

Чтобы не передавать получение `traceId` в каждый вызов конструктора, можно добавить вспомогательный метод:

```java
public final class TraceIdProvider {

    private TraceIdProvider() {
    }

    public static String currentTraceId() {
        return MDC.get("traceId");
    }
}
```

Использование:

```java
throw new RequisitionNotFoundException(
        TraceIdProvider.currentTraceId()
);
```

Либо получение текущего `traceId` можно реализовать непосредственно в базовом исключении, если это соответствует принятым в проекте соглашениям.

---

### Обработка ошибок валидации

Так как `DomainExceptionHandler` наследуется от `ResponseEntityExceptionHandler`, ошибки Spring MVC рекомендуется обрабатывать через переопределение соответствующих методов.

Например:

```java
@Override
protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
) {
    String message = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error ->
                    error.getField()
                            + ": "
                            + error.getDefaultMessage()
            )
            .collect(Collectors.joining("; "));

    ApiError apiError = ApiError.of(
            message,
            MDC.get("traceId")
    );

    return ResponseEntity
            .badRequest()
            .body(apiError);
}
```

Ошибки валидации не обязательно включать в иерархию `DomainException`, поскольку они создаются Spring до вызова бизнес-логики.

---

### Изменения в сервисном слое

Контроллер не должен самостоятельно анализировать ответ внешней системы.

Текущая логика:

```java
if (requisitionResponse == null) {
    throw new HttpResponseException(...);
}

if (!requisitionResponse.getResponseCode().startsWith("2")) {
    throw new HttpResponseException(...);
}
```

Должна быть перенесена в сервис или интеграционный адаптер.

Пример:

```java
private RequisitionGeneralCandidateDto validateResponse(
        RequisitionGeneralCandidateDto response
) {
    if (response == null) {
        throw new ExternalSystemUnavailableException(
                "Внешняя система не вернула ответ",
                TraceIdProvider.currentTraceId()
        );
    }

    HttpStatus status = parseStatus(
            response.getResponseCode(),
            response.getTraceId()
    );

    if (!status.is2xxSuccessful()) {
        throw new ExternalSystemResponseException(
                status,
                response.getErrorMessage(),
                response.getTraceId()
        );
    }

    return response;
}
```

Некорректный `responseCode` должен приводить к доменному интеграционному исключению:

```java
private HttpStatus parseStatus(
        String responseCode,
        String traceId
) {
    if (responseCode == null
            || !responseCode.matches("\\d{3}.*")) {

        throw new InvalidExternalSystemResponseException(
                "Внешняя система вернула некорректный код ответа",
                traceId
        );
    }

    int statusCode = Integer.parseInt(
            responseCode.substring(0, 3)
    );

    try {
        return HttpStatus.valueOf(statusCode);
    } catch (IllegalArgumentException ex) {
        throw new InvalidExternalSystemResponseException(
                "Внешняя система вернула неизвестный HTTP-статус: "
                        + statusCode,
                traceId
        );
    }
}
```

---

### Целевой контроллер

После выполнения задачи контроллер не должен содержать обработку ошибок:

```java
@PostMapping("/requisitions/comments")
public ResponseEntity<RequisitionGeneralCandidateDtoOut>
postRequisitionAddComment(
        @Valid
        @RequestBody RequisitionAddCommentRequest requestDto,
        @SapSystemParams SapSystemParamsDto systemParams
) {
    RequisitionGeneralCandidateDtoOut response =
            requisitionService.addComment(
                    systemParams,
                    requestDto
            );

    return ResponseEntity.ok(response);
}
```

Контроллер:

* не анализирует `responseCode`;
* не создаёт ошибочный ответ;
* не определяет HTTP-статус ошибки;
* не логирует исключение;
* не работает с `HttpResponseException`.

---

### Что вынести в `commons`

В общий модуль рекомендуется вынести:

* `DomainException`;
* базовые инфраструктурные исключения;
* `ApiError`;
* `DomainExceptionHandler`;
* механизм получения `traceId`;
* общую обработку ошибок валидации.

Доменные исключения конкретного сервиса следует оставить в самом сервисе.

Например:

```text
commons
├── DomainException
├── ExternalSystemException
├── RequiredSystemParameterMissingException
├── ApiError
└── DomainExceptionHandler
```

```text
requisition-service
├── RequisitionNotFoundException
├── InvalidRequisitionStateException
└── OperationNotAllowedException
```

Так `commons` не будет зависеть от предметной области конкретного сервиса.

---

### Критерии приёмки

* Создан базовый класс `DomainException`.
* Ожидаемые исключения приложения наследуются от `DomainException`.
* Созданы отдельные исключения для бизнес- и интеграционных ошибок.
* `HttpResponseException` больше не используется в рефакторируемом сценарии.
* Создан единый `DomainExceptionHandler`.
* Все наследники `DomainException` обрабатываются одним методом.
* Для ошибок `4xx` используется уровень логирования `WARN`.
* Для ошибок `5xx` используется уровень логирования `ERROR`.
* Для непредвиденных исключений клиент получает `500 Internal Server Error`.
* Технические детали и stack trace не возвращаются клиенту.
* Все ошибки возвращаются в формате `ApiError`.
* Контроллер не содержит логики обработки ошибок.
* Разбор ответа внешней системы перенесён в сервис или интеграционный адаптер.
* Добавлены unit-тесты иерархии исключений.
* Добавлены тесты `DomainExceptionHandler`.
* Добавлены тесты обработки ошибок `4xx`, `5xx` и непредвиденного исключения.
* Добавлен тест обработки ошибки валидации входного DTO.
