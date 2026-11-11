# ADR-002: Перенос SAP-интеграции с централизованной обработкой ошибок и аудитом без RequestContext

- **Статус:** Proposed
- **Дата:** 2026-11-11
- **Область:** перенос `GET /event` (`easup_payavg_event`) в целевой SRHR-микросервис
- **Заменяет проектное решение:** ADR-001 (вариант с `RequestContext`)
- **Ограничения:** старые эндпоинты и их обработчик не изменяем; клиент интеграции целевого сервиса рассматривается отдельно

## 1. Контекст и границы задачи

Переносим интеграционный endpoint `GET /event` из исходного сервиса. В исходной реализации контроллер:

```java
@GetMapping(value = "/event", produces = "application/json")
public ResponseEntity<Object> getPayavgEvent(HttpServletRequest request) throws Exception {
    SapSystemParamsDto systemParams = setSystemParams(request);
    setAttributes(request, "easup_payavg_event");
    PayavgEventDto dto = easupPaystubPayavgIntegrationService.getPayavgEvent(systemParams);
    if (dto != null) {
        return ResponseEntity.ok(dto);
    }
    throw new HttpResponseException(404, ErrorResponse.EASUP_REQUEST_NOT_FOUND);
}
```

`setSystemParams()` собирает из заголовков `pernr`, `sessionId`, `role`, `Roles`, `logId`, `realm` объект `SapSystemParamsDto`, необходимый интеграции.

`setAttributes()` повторно копирует `sessionId`, `pernr`, `logId` (под именем `traceId`) и `X-user-login` (под именем `adlogin`) в атрибуты сервлетного запроса и добавляет `requestType`.

Существующий `EasupIntegrationExceptionHandler` читает эти атрибуты, формирует `ServiceErrorDto`, логирует и через большие `switch` отправляет аудит разными методами для unexpected / forbidden / not found. Из исходного обработчика также известны особые правила, например исключение статуса 510 из части аудита. Их нельзя терять незаметно.

**За пределами ADR:** SSL, сертификаты, авторизация, заголовки, вызов `RestTemplate` и устройство существующего `EasupClient` целевого сервиса; возможная замена или обновление TSAU.

## 2. Проблемы исходного подхода

1. **Скрытый контракт контроллер → advice.** Обработчик ошибок зависит от `setAttributes()` в теле контроллера; забытый вызов приводит к пропуску/неполному аудиту.
2. **Недоступность данных при ранних ошибках.** Исключения до выполнения `setAttributes()` оставляют атрибуты пустыми.
3. **Дублирование данных.** Заголовки читаются отдельно в `SapSystemParamsDto` и набор атрибутов запроса.
4. **Смешение обработки ошибок с маршрутизацией сотен событий.** Три большие группы `switch (requestType)` сложно сопровождать.
5. **Нетипизированные исключения.** Текст ошибки анализируется через `contains("SAP")`, а `NullPointerException` превращается в 404; возникают неоднозначности в контракте.
6. **Риски в старой реализации.** Невалидный/отсутствующий `requestType` приводит к молчаливому пропуску; отдельные ветки `switch` могут проваливаться в следующие `case` при отсутствии `break`.

## 3. Принятое решение

**Не создаём `RequestContext`, `RequestContextInterceptor` и `RequestContextArgumentResolver`.** Используем существующий общий для команды `SapSystemParamsDto` для сервисного/интеграционного вызова.

- `@SapSystemParams SapSystemParamsDto` сохраняется как явный контракт входных параметров контроллера и существующего `HandlerMethodArgumentResolver`.
- `@RequestType("easup_payavg_event")` маркирует метод контроллера.
- Новый, ограниченный новым пакетом `@RestControllerAdvice` при ошибке извлекает метаданные метода контроллера через `HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE`, а поля аудита — из проверенных данных запроса (`HttpServletRequest` / доверенный security context).
- `AuditSenderResolver` выбирает реализацию `AuditSender` через `supports(requestType)`.
- У каждого `AuditSender` три метода для категорий **UNEXPECTED**, **FORBIDDEN**, **NOT_FOUND**.
- Аудит **остаётся инициируемым из `ControllerAdvice`**, но логика конкретной операции живёт в отдельном `AuditSender`.
- Внешние технические ошибки должны преобразовываться в типизированные исключения на границе интеграции, а `ControllerAdvice` формирует `ServiceErrorDto`.
- Старый глобальный advice не переписываем. Новый ограничиваем целевыми контроллерами и задаём более высокий приоритет; обязательно тестируем разрешение конфликтов.

### Почему оставляем `@SapSystemParams`

Технически собственный `HandlerMethodArgumentResolver` может выбирать аргумент по типу `SapSystemParamsDto` без аннотации. Однако в Spring MVC неаннотированный сложный аргумент может обрабатываться стандартным `ModelAttribute` resolver (приоритет и конфигурация имеют значение). Поэтому **безопасное решение для миграции — сохранить аннотацию `@SapSystemParams`**: она явно связывает параметр с вашим resolver, привычна команде и не требует менять его поведение. Отказ от аннотации — отдельное необязательное решение после тестов порядка resolver-ов.

## 4. Целевая цепочка

```text
Входящий HTTP-запрос
    |
    +-- Spring Security / servlet filters (до MVC)
    |
    v
Spring MVC выбирает HandlerMethod (@RequestType)
    |
    v
Существующий SapSystemParamsArgumentResolver
    |  [заголовки → SapSystemParamsDto]
    v
Controller(@SapSystemParams SapSystemParamsDto)
    |
    v
Service → Integration Adapter → существующий клиент целевого сервиса
    |
    +-- Успех → PayavgEventDto, HTTP 200
    |
    +-- Ошибка → типизированное исключение
                  |
                  v
          Новый @RestControllerAdvice
           |   ├─ HandlerMethod → @RequestType
           |   ├─ запрос / trusted principal → поля аудита
           |   ├─ AuditSenderResolver.supports(requestType)
           |   ├─ sendUnexpectedError / sendForbidden / sendNotFound
           |   └─ ServiceErrorDto + согласованный HTTP-статус
           v
        HTTP response
```

**Ограничение:** 401/403, которые генерирует Spring Security **до MVC**, не обязаны попадать в `@ControllerAdvice`, а `HandlerMethod` ещё может быть неизвестен. Для них нужен отдельный `AuthenticationEntryPoint` / `AccessDeniedHandler`, если такие ошибки требуется аудировать. Не путать их с 401/403, полученными **от SAP после входа в контроллер**.

## 5. Этап A: аннотация `@RequestType`

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestType {
    String value();
}
```

Аннотация задаёт **идентификатор операции**, а не сам аудит. Изменение её значения меняет маршрутизацию к отправителю; оно должно быть согласовано с аудиторским контрактом.

## 6. Этап B: контроллер

Предлагаемый компактный вариант:

```java
@RestController
@RequiredArgsConstructor
public class PayavgController {
    private final PayavgService payavgService;

    @RequestType("easup_payavg_event")
    @GetMapping(value = "/event", produces = "application/json")
    public PayavgEventDto getPayavgEvent(
            @SapSystemParams SapSystemParamsDto systemParams
    ) {
        return payavgService.getPayavgEvent(systemParams);
    }
}
```

**Изменение:** нет `HttpServletRequest`, `setSystemParams()`, `setAttributes()`, `ResponseEntity<Object>` и проверки `null` в контроллере. HTTP 200 для успешного DTO обеспечит Spring MVC. `null → 404` должен обеспечивать сервис через прикладное исключение. Путь и возможные префиксы `@RequestMapping` надо сверить с реальными маршрутами целевого сервиса.

Существующий resolver, формирующий `SapSystemParamsDto`, сохраняем. Не создаём второй resolver только ради ADR.

## 7. Этап C: сервис, интеграция и исключения

Сервис отвечает за смысл результата, интеграционный адаптер — за преобразование технических сбоев клиента.

```java
public PayavgEventDto getPayavgEvent(SapSystemParamsDto systemParams) {
    PayavgEventDto result = integrationPort.getPayavgEvent(systemParams);
    if (result == null) {
        throw new PayavgEventNotFoundException();
    }
    return result;
}
```

Иллюстративные типы:

```java
public class PayavgEventNotFoundException extends RuntimeException {
    public PayavgEventNotFoundException() {
        super("Данные события среднего заработка не найдены");
    }
}

public class ExternalSystemException extends RuntimeException {
    private final int upstreamStatus;

    public ExternalSystemException(int upstreamStatus, String message, Throwable cause) {
        super(message, cause);
        this.upstreamStatus = upstreamStatus;
    }

    public int getUpstreamStatus() { return upstreamStatus; }
}

public class ExternalSystemUnavailableException extends RuntimeException {
    public ExternalSystemUnavailableException(Throwable cause) {
        super("Внешняя система недоступна", cause);
    }
}
```

На границе интеграции (пример без изменения целевого `EasupClient`):

```java
try {
    return client.getPayavgEvent(systemParams);
} catch (HttpStatusCodeException ex) {
    throw new ExternalSystemException(
            ex.getStatusCode().value(), "Ошибка SAP", ex);
} catch (ResourceAccessException ex) {
    throw new ExternalSystemUnavailableException(ex);
}
```

Точный код адаптера зависит от клиента в целевом сервисе. При необходимости отдельно классифицируем timeout, ошибки десериализации и ошибки сертификатов. Не следует автоматически выдавать клиенту SAP 401: upstream-401 и client-401 имеют разный смысл.

## 8. Этап D: определение requestType в ControllerAdvice

**Не нужен `RequestContext` и не нужен interceptor.** После выбора MVC-обработчика Spring хранит его в атрибуте `HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE`.

```java
private Optional<String> resolveRequestType(HttpServletRequest request) {
    Object handler = request.getAttribute(
            HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);

    if (!(handler instanceof HandlerMethod method)) {
        return Optional.empty();
    }
    RequestType annotation = method.getMethodAnnotation(RequestType.class);
    return annotation == null
            ? Optional.empty()
            : Optional.of(annotation.value());
}
```

Надо учитывать `null`, обычные ресурсы, ранние ошибки, ошибки разрешения маршрута, а также ограничения уровня Spring Security. Если метод контроллера уже был выбран, но произошла ошибка в argument resolver, метаданные обработчика обычно доступны; это подтверждаем тестом для целевой конфигурации.

### Откуда брать pernr, sessionId, adlogin и traceId?

Для **минимального переноса** новый advice может прочитать HTTP-заголовки непосредственно, не создавая отдельного контейнера контекста:

```java
String pernr = request.getHeader("pernr");
String sessionId = request.getHeader("sessionId");
String adLogin = request.getHeader("X-user-login");
String traceId = request.getHeader("logId");
```

Это технически проще `setAttributes()`, но источник полей должен быть доверенным. При наличии проверенного JWT/SecurityContext предпочтительнее брать идентификатор субъекта оттуда. Для корреляции по возможности используем текущий OpenTelemetry trace id, а `logId` — лишь совместимый входной идентификатор при согласованном контракте. MDC не является хранилищем бизнес-параметров; если нужен в логах, наполняем его отдельно.

Для сокращения повторов допускается **локальная функция** чтения полей внутри компонента аудита, но **отдельный RequestContext в этой версии ADR не вводим**.

## 9. Этап E: контракт AuditSender и Resolver

`AuditSender` обслуживает **один requestType** и реализует три категории ошибок. Методы получают те параметры, которые реально нужны старым отправителям, без нового общего контекстного DTO.

```java
public interface AuditSender {
    boolean supports(String requestType);

    void sendUnexpectedError(LocalDateTime time, String pernr,
                             String sessionId, String adLogin);
    void sendForbidden(LocalDateTime time, String pernr,
                       String sessionId, String adLogin);
    void sendNotFound(LocalDateTime time, String pernr,
                      String sessionId, String adLogin);
}
```

Пример конкретного отправителя:

```java
@Component
@RequiredArgsConstructor
public class PayavgEventAuditSender implements AuditSender {
    private final AuditEventSenderCustom sender;

    @Override
    public boolean supports(String requestType) {
        return "easup_payavg_event".equals(requestType);
    }

    @Override
    public void sendUnexpectedError(LocalDateTime time, String pernr,
                                    String sessionId, String adLogin) {
        sender.sendEasupPayavgEventError(time, pernr, sessionId, adLogin);
    }

    @Override
    public void sendForbidden(LocalDateTime time, String pernr,
                              String sessionId, String adLogin) {
        sender.sendEasupPayavgEventForbidden(time, pernr, sessionId, adLogin);
    }

    @Override
    public void sendNotFound(LocalDateTime time, String pernr,
                             String sessionId, String adLogin) {
        sender.sendEasupPayavgEventNotFound(time, pernr, sessionId, adLogin);
    }
}
```

Существующий `AuditEventSenderCustom` используем как делегат, если он доступен в целевом сервисе; если нет — реализуем адаптер к реально используемой там аудиторской библиотеке без изменения публичного контракта `AuditSender`.

**Resolver** выбирает ровно одну реализацию; отсутствие или несколько совпадений нельзя молча игнорировать:

```java
@Component
@RequiredArgsConstructor
public class AuditSenderResolver {
    private final List<AuditSender> senders;

    public AuditSender resolve(String requestType) {
        List<AuditSender> matches = senders.stream()
                .filter(sender -> sender.supports(requestType))
                .toList();

        if (matches.size() != 1) {
            throw new IllegalStateException(
                    "Expected exactly one AuditSender for " + requestType
                    + ", found " + matches.size());
        }
        return matches.get(0);
    }
}
```

Вариант с контролируемым пропуском при отсутствии отправителя допустим только при явном продуктово-аудиторском решении. В новой архитектуре предпочтительнее обнаруживать пропуски тестами и мониторингом. Ошибку выбора/отправки аудита перехватываем отдельно, чтобы не подменять исходный HTTP-ответ; обязательно логируем/метрифицируем.

## 10. Этап F: отдельный ControllerAdvice с аудитом

Новый advice применяем только к новым контроллерам (пример — пакет `ru.ntdev.srhr.ms.easup.v2.controller`; фактический пакет адаптировать). В целевом сервисе старый advice может быть глобальным и иметь `@ExceptionHandler(Exception.class)`; **поэтому задаём новый advice с более высоким приоритетом и полноценным fallback**. Приоритет не заменяет интеграционные тесты, а строгая изоляция требует ограничения области старого advice, если это когда-нибудь станет возможно.

```java
@RestControllerAdvice(basePackages = "ru.ntdev.srhr.ms.easup.v2.controller")
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class PayavgExceptionHandler {
    private final AuditSenderResolver auditSenderResolver;

    // @ExceptionHandler(PayavgEventNotFoundException.class) → 404 + NOT_FOUND
    // @ExceptionHandler(ExternalSystemException.class) → согласованный статус/категория
    // @ExceptionHandler(ExternalSystemUnavailableException.class) → 503 + UNEXPECTED
    // @ExceptionHandler(Exception.class) → 500 + UNEXPECTED
}
```

**Схематичный маршрут `NOT_FOUND`:**

```java
@ExceptionHandler(PayavgEventNotFoundException.class)
public ResponseEntity<ServiceErrorDto> handleNotFound(
        PayavgEventNotFoundException ex, HttpServletRequest request) {
    sendAudit(request, AuditErrorType.NOT_FOUND);
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(createError(ex, request));
}
```

В `sendAudit`:

```java
resolveRequestType(request).ifPresent(requestType -> {
    try {
        AuditSender sender = auditSenderResolver.resolve(requestType);
        // В реальном коде выбрать один из трёх методов по AuditErrorType;
        // pernr/sessionId/adLogin брать из проверенного запроса/identity.
    } catch (Exception auditEx) {
        log.error("Audit failed for requestType={}", requestType, auditEx);
    }
});
```

Этот фрагмент намеренно показывает только главную связку, а не полный готовый класс. В полноценной реализации также логируем отсутствие `requestType`, гарантируем отправку правильной категории и **не используем `requestType`/заголовки без проверок на `null`**.

### Формирование `ServiceErrorDto`

Используем существующие `ServiceError` и `ServiceErrorDto` с согласованным форматом ответа (`message`, `code`, `logId`/traceId, `details`). **Не отдаём клиенту текст внутренних исключений, стектрейсы или сырые SAP-ответы.** Создание ответа выносим в приватный метод/фабрику и тестируем отдельно.

Пример логики для одной категории:

```java
String traceId = Optional.ofNullable(request.getHeader("logId"))
        .orElse("undefined");
ServiceErrorDto dto = new ServiceErrorDto(
        new ServiceError("Данные не найдены", "404", traceId, List.of()));
```

Здесь пример использует сигнатуру конструктора из старого обработчика. Уточнить поведение и требуемые поля на DTO из целевого сервиса.

## 11. Классификация ошибок и контракт HTTP

**Не приравниваем статус от SAP к статусу нашего API без проверки контракта.** Предлагаемая матрица:

| Источник/условие | Типизированное исключение | Категория аудита | HTTP клиенту (предварительно) |
|---|---|---|---|
| DTO `null` | `PayavgEventNotFoundException` | NOT_FOUND | 404 |
| SAP 404 с подтверждённой семантикой отсутствия данных | интеграционная NotFound / маппинг в прикладную | NOT_FOUND | 404 |
| SAP 401/403 как ошибочный ответ upstream | `ExternalSystemException` | FORBIDDEN — **только если нужен старый контракт аудита** | согласовать: часто 502 вместо 401/403 |
| SAP 500 | `ExternalSystemException` | UNEXPECTED | согласовать: часто 502 |
| Сетевой сбой | `ExternalSystemUnavailableException` | UNEXPECTED | 503 |
| Таймаут | отдельный тип или причина недоступности | UNEXPECTED | 504 по контракту |
| Баг в сервисе (NPE) | исходное исключение | UNEXPECTED | 500 (не 404) |
| Spring Security 401/403 до MVC | обработчики Spring Security | отдельный путь аудита, если обязателен | 401/403 |
| SAP 510 | определить по старому контракту | возможно исключение из аудита | согласовать |

При переносе **сначала фиксируем фактическое поведение**, особенно SAP 401/403/404/500/510 и формат ошибок. Изменение API-контракта оформляем отдельным согласованным решением.

## 12. Параллельная работа старой и новой реализации

- Старый `EasupIntegrationExceptionHandler` остаётся глобальным и неизменным.
- Новый advice ограничен пакетом новых контроллеров и имеет больший приоритет.
- Для новых контроллеров новый advice содержит обработчики всех целевых исключений и fallback для unexpected.
- Для старых контроллеров новый advice неприменим, продолжает работать старый.
- Нельзя считать два `@ControllerAdvice` полностью изолированными только за счёт `@Order`: исключения вне MVC, из argument resolver и поведение других `@ControllerAdvice` проверяем тестами.
- Не переносим в новый сервис старые многотысячные `switch`; добавляем только требуемый sender операции `easup_payavg_event`.

## 13. Пошаговая миграция

1. **Зафиксировать baseline.** Записать успешный ответ, `null → 404`, ошибки SAP, формат `ServiceErrorDto`, нужные события аудита, особое правило 510.
2. **Подготовить `@RequestType`.** Аннотировать новый endpoint, не менять старые.
3. **Переиспользовать `@SapSystemParams` resolver.** Убедиться, что он зарегистрирован и формирует нужные поля; без аннотации пока не переходить.
4. **Перенести контроллер и сервис.** Контроллер только делегирует, сервис проверяет `null`, существующий клиент адаптируем позже.
5. **Добавить типизированные исключения и адаптер ошибок** на границе интеграции (клиент отдельно).
6. **Реализовать `AuditSender`, `AuditSenderResolver`.** Для `easup_payavg_event`: три варианта аудита, тест поддержки requestType и уникальности sender.
7. **Добавить scoped `@RestControllerAdvice`.** Читает requestType из HandlerMethod; получает параметры аудита из запроса или доверенной identity; выбирает sender; формирует ServiceErrorDto.
8. **Написать интеграционные тесты.** Проверить новый и старый маршруты, audit, ошибки и приоритет advice.
9. **Переключить трафик.** После сверки контрактов; при необходимости предусмотрен rollback на старый endpoint.

## 14. Тестовый план

### Unit tests

- `AuditSenderResolver`: один поддерживающий sender; ноль; два; `supports` с чужим `requestType`.
- `PayavgEventAuditSender`: корректный вызов `Error` / `Forbidden` / `NotFound` в `AuditEventSenderCustom`.
- `PayavgService`: обычный DTO; `null → PayavgEventNotFoundException`.
- Классификация `ExternalSystemException`: SAP 401/403/404/500/510 и сбой сети согласно согласованной матрице.

### MockMvc / Spring Boot tests

- Новый endpoint: `@RequestType` определяется в MVC, `SapSystemParamsDto` заполняется из заголовков, вызывает сервис.
- Отказ в argument resolver: новый advice выполняется при доступных метаданных HandlerMethod, ошибки не маскируются.
- `PayavgEventNotFoundException`: HTTP 404, правильный `ServiceErrorDto`, один `sendNotFound`.
- Ошибка SAP 401/403: корректная категория аудита, согласованный статус клиенту.
- Ошибка SAP 500, сетевой сбой, NPE: `UNEXPECTED`, безопасный ответ.
- При ошибке аудита исходный HTTP-статус и ответ сохраняются, ошибка аудита залогирована.
- Другой `requestType`: нужен другой sender, не `PayavgEventAuditSender`.
- Старый endpoint без `@RequestType`: работает старый `ControllerAdvice`, новый не вызывается.
- Spring Security 401/403 до MVC: подтверждаем, что новый advice не ошибочно ожидается на этом пути.

### WireMock tests (после согласования клиента)

- SAP отвечает 200/404/401/403/500/510, таймаут, некорректный JSON; проверяем контракт приложения и аудит.

## 15. Альтернативы и причины отказа

| Альтернатива | Почему не выбрана сейчас |
|---|---|
| `RequestContext` + interceptor + resolver | Добавляет вторую модель контекста и новые компоненты при уже распространённом `SapSystemParamsDto` |
| Копировать `setAttributes()` в новый контроллер | Сохраняет скрытую зависимость и проблемы ранних ошибок |
| Перенести весь старый advice и `switch` | Высокая связность, дублирование и большой риск ошибок |
| Сделать отдельный AOP-аудит | По принятому требованию аудит должен остаться в `ControllerAdvice` |
| Убрать `@SapSystemParams` | Может поменять способ разрешения аргументов Spring MVC; команда привыкла к аннотации |

## 16. Последствия, риски и критерии приёмки

**Плюсы:** меньше логики в контроллере; сохраняется общая для команды модель `SapSystemParamsDto`; отсутствует необходимость копировать заголовки в request attributes; добавление новой операции требует нового sender, а не изменения гигантских `switch`; новый и старый подходы могут сосуществовать.

**Компромиссы:** новый advice непосредственно читает параметры запроса; это допустимо для минимальной миграции, но требует единых правил доверия заголовкам. Метаданные `HandlerMethod` недоступны для всех видов ранних ошибок. Сбой аудита не должен маскировать клиентскую ошибку, но пропуск аудита должен быть наблюдаемым. Типизированные исключения и правила трансляции upstream-статусов могут требовать согласования API.

**Definition of Done:**

- [ ] У нового endpoint нет `setAttributes()` и ручного `setSystemParams()`.
- [ ] Новый контроллер принимает `@SapSystemParams SapSystemParamsDto`.
- [ ] Идентификатор операции задаётся `@RequestType`.
- [ ] Новый advice получает `requestType` по выбранному MVC `HandlerMethod` и корректно обрабатывает его отсутствие.
- [ ] Аудит из advice идёт через `AuditSenderResolver.supports(requestType)`.
- [ ] Для события `easup_payavg_event` подтверждены три варианта отправки аудита.
- [ ] Существующие эндпоинты, старый advice и существующий клиент не меняются в рамках задачи.
- [ ] HTTP-контракт и аудит проверены тестами, включая 401/403 от SAP и Security до MVC, 404, 500, 510.
- [ ] При отказе аудита бизнес-ответ не теряется, а проблема диагностируется.

---

### Замечание о статусе документа

Это **предлагаемый ADR**, а не утверждение, что все демонстрационные классы и методы уже имеются в целевом сервисе. Конкретные пакеты, интерфейс интеграционного клиента, сигнатуры аудита и маппинг HTTP-статусов требуется сверить перед реализацией. Код приведён как архитектурные примеры, чтобы команда могла оценить решение и внедрять его по шагам.
