# ADR-001: Централизация обработки интеграционных ошибок и аудита

**Статус:** Proposed

## Контекст

В существующей реализации контроллеры вручную формируют `SapSystemParamsDto` и записывают данные HTTP-запроса в атрибуты `HttpServletRequest` через `setAttributes()`.

Эти атрибуты используются `EasupIntegrationExceptionHandler` для формирования ошибок, логирования и отправки событий аудита.

Текущий подход имеет несколько недостатков:

1. Контроллеры содержат инфраструктурную логику подготовки параметров интеграции и аудита.
2. Обработка исключений зависит от предварительного выполнения `setAttributes()` в контроллере.
3. `EasupIntegrationExceptionHandler` совмещает формирование HTTP-ответов, логирование и маршрутизацию событий аудита.
4. Для определения события аудита используются многочисленные конструкции `switch (requestType)`.
5. В обработчике присутствует дублирование логики извлечения атрибутов запроса и формирования ошибок.

## Решение

Предлагается внедрить новый механизм обработки интеграционных ошибок для переносимых и новых эндпоинтов.

1. Сохранить существующую реализацию без изменений.
2. Использовать `@SapSystemParams` для автоматического формирования `SapSystemParamsDto` через `HandlerMethodArgumentResolver`.
3. Ввести аннотацию `@RequestType` для идентификации операции.
4. Получать параметры аудита непосредственно из HTTP-запроса, без предварительного копирования заголовков в атрибуты.
5. Использовать типизированные исключения для обработки интеграционных ошибок.
6. Создать отдельный `@RestControllerAdvice` для новых эндпоинтов.
7. Оставить инициирование аудита в `ControllerAdvice`, делегировав выбор и отправку событий через `AuditSenderResolver` и `AuditSender`.

## Предлагаемая реализация

### 1. Контроллер

**Текущая реализация:**

```java
@GetMapping(value = "/event", produces = "application/json")
public ResponseEntity<Object> getPayavgEvent(
        HttpServletRequest request
) throws Exception {

    SapSystemParamsDto systemParams = setSystemParams(request);
    setAttributes(request, "easup_payavg_event");

    PayavgEventDto dto =
            easupPaystubPayavgIntegrationService.getPayavgEvent(systemParams);

    if (dto != null) {
        return ResponseEntity.ok(dto);
    }

    throw new HttpResponseException(
            404,
            ErrorResponse.EASUP_REQUEST_NOT_FOUND
    );
}
```

**Предлагаемая реализация:**

```java
@RequestType("easup_payavg_event")
@GetMapping(value = "/event", produces = "application/json")
public PayavgEventDto getPayavgEvent(
        @SapSystemParams SapSystemParamsDto params
) {
    return payavgService.getPayavgEvent(params);
}
```

Аннотация `@RequestType` определяет операцию, а `@SapSystemParams` обеспечивает формирование параметров интеграции.

Контроллер больше не отвечает за подготовку данных аудита и обработку ошибок.

### 2. Идентификация операции

Вводится аннотация:

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestType {
    String value();
}
```

`ControllerAdvice` определяет операцию через `HandlerMethod`:

```java
Object handler = request.getAttribute(
        HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE
);

if (handler instanceof HandlerMethod handlerMethod) {
    RequestType annotation =
            handlerMethod.getMethodAnnotation(RequestType.class);

    if (annotation != null) {
        return annotation.value();
    }
}
```

Это позволяет отказаться от ручного вызова `setAttributes()`.

Данные `sessionId`, `pernr`, `adlogin` и `traceId` могут получаться непосредственно из заголовков текущего HTTP-запроса.

### 3. Обработка интеграционных ошибок

Ошибки интеграции преобразуются в типизированные исключения.

Например:

- `PayavgEventNotFoundException` — данные не найдены.
- `ExternalSystemException` — внешняя система вернула ошибку.
- `ExternalSystemUnavailableException` — внешняя система недоступна.

Прикладной сервис отвечает за проверку результата выполнения операции:

```java
public PayavgEventDto getPayavgEvent(SapSystemParamsDto params) {
    PayavgEventDto result = integrationService.getPayavgEvent(params);

    if (result == null) {
        throw new PayavgEventNotFoundException();
    }

    return result;
}
```

Технические ошибки HTTP-клиента преобразуются в интеграционном слое. Настройка и реализация существующего клиента рассматриваются отдельно.

### 4. ControllerAdvice

Для новых эндпоинтов создаётся отдельный `@RestControllerAdvice`.

Он отвечает за:

- обработку типизированных исключений;
- формирование `ServiceErrorDto`;
- логирование ошибок;
- инициирование отправки событий аудита.

Пример:

```java
@ExceptionHandler(PayavgEventNotFoundException.class)
public ResponseEntity<ServiceErrorDto> handleNotFound(
        PayavgEventNotFoundException ex,
        HttpServletRequest request
) {
    auditDispatcher.sendSafely(request, AuditErrorType.NOT_FOUND);

    return buildErrorResponse(
            HttpStatus.NOT_FOUND,
            ex.getMessage(),
            request
    );
}
```

Новый обработчик ограничивается новыми контроллерами и получает более высокий приоритет относительно существующего глобального `ControllerAdvice`.

### 5. Маршрутизация аудита

Для выбора отправителя аудита используется подход Strategy + Resolver.

```java
public interface AuditSender {

    boolean supports(String requestType);

    void sendUnexpectedError(AuditEventData data);

    void sendForbidden(AuditEventData data);

    void sendNotFound(AuditEventData data);
}
```

`AuditSenderResolver` выбирает реализацию по `supports(requestType)`.

Каждая реализация `AuditSender` отвечает за события конкретной операции.

Это позволяет избавиться от больших конструкций `switch` в существующем обработчике исключений.

При ошибке отправки аудита исходный HTTP-ответ не должен изменяться. Ошибка аудита логируется отдельно.

## Ограничения

- Не изменять поведение существующих эндпоинтов.
- Сохранить согласованный HTTP-контракт и формат `ServiceErrorDto`.
- Сохранить обязательные события аудита.
- Не менять существующую реализацию интеграционного HTTP-клиента в рамках данного решения.
- Обеспечить корректное сосуществование старого и нового `ControllerAdvice`.
- Внедрять изменения поэтапно с интеграционным тестированием.

## Последствия

**Положительные:**

- Упрощение контроллеров.
- Устранение ручного заполнения атрибутов запроса.
- Централизация обработки интеграционных ошибок.
- Устранение дублирования логики аудита.
- Возможность добавлять новые операции без изменения центрального обработчика.
- Сохранение привычного для команды `SapSystemParamsDto`.

**Ограничения решения:**

- Идентификация операции через `HandlerMethod` доступна только после определения обработчика Spring MVC.
- Ошибки Spring Security, возникающие до выполнения MVC, требуют отдельной обработки.
- Для изоляции нового `ControllerAdvice` от существующего глобального обработчика необходимо проверить приоритеты и область действия.
- Соответствие HTTP-статусов внешней системы ответам API должно быть зафиксировано отдельно.

## Проверка

Реализация должна быть покрыта интеграционными тестами с использованием MockMvc и WireMock.

Необходимо проверить успешное выполнение операции, ошибки 401/403/404/500, отсутствие данных, недоступность внешней системы, маршрутизацию аудита и отсутствие влияния на существующие эндпоинты.