# ADR-001: Перенос интеграционного эндпоинта на RequestContext и маршрутизацию аудита

- **Статус:** Proposed
- **Дата:** 2026-11-11
- **Область:** перенос `GET /event` (`easup_payavg_event`) в целевой SRHR-микросервис
- **Принцип миграции:** новая реализация существует параллельно со старой; существующие эндпоинты целевого сервиса не изменяются.
- **Вне рамок:** замена/рефакторинг `EasupClient` целевого сервиса, изменение протокола SAP, полная миграция остальных эндпоинтов.

## 1. Контекст и исходная проблема

В исходном сервисе метод контроллера выполняет сразу несколько задач:

```java
@GetMapping(value = "/event", produces = "application/json")
public ResponseEntity<Object> getPayavgEvent(HttpServletRequest request) throws Exception {
    SapSystemParamsDto params = setSystemParams(request);
    setAttributes(request, "easup_payavg_event");

    PayavgEventDto dto = easupPaystubPayavgIntegrationService.getPayavgEvent(params);
    if (dto != null) {
        return ResponseEntity.ok(dto);
    }
    throw new HttpResponseException(404, ErrorResponse.EASUP_REQUEST_NOT_FOUND);
}
```

`setSystemParams()` читает `pernr`, `sessionId`, `role`, `Roles`, `logId`, `realm` в DTO для интеграции. `setAttributes()` повторно читает часть заголовков и устанавливает `requestType`, `sessionId`, `pernr`, `adlogin`, `traceId` в `HttpServletRequest` для обработчика ошибок.

Старый `EasupIntegrationExceptionHandler` считывает эти атрибуты, формирует `ServiceErrorDto`, логирует ошибку и выбирает событие аудита через большие `switch (requestType)` для разных категорий ошибок.

### Недостатки

1. Контроллер знает внутренний формат контекста для аудита и ошибки.
2. Данные заголовков читаются и представляются в двух местах; легко получить рассинхронизацию.
3. Контекст отсутствует, если сбой произошёл до `setAttributes()` — например, во время разрешения аргументов метода.
4. Обработчик ошибок совмещает формирование HTTP-ответа, классификацию интеграционных ошибок, логирование и аудит.
5. Маршрутизация аудита через `switch` требует правок в центральном классе при добавлении каждой операции.
6. Определение ошибок через `e.getMessage().contains("SAP")` ненадёжно; превращение любого `NullPointerException` в 404 маскирует дефекты.
7. Глобальный `@ExceptionHandler(Exception.class)` в старом advice создаёт риск пересечения обработчиков при параллельной миграции.

## 2. Решение

Для **новых переносимых эндпоинтов** вводим:

- `@RequestType` — декларативный идентификатор операции.
- `RequestContext` — неизменяемый объект с метаданными запроса и полями, необходимыми для интеграции.
- `RequestContextInterceptor` — формирует контекст из заголовков и аннотации перед выполнением контроллера, сохраняет его одним request attribute.
- `RequestContextArgumentResolver` — передаёт **тот же объект** в аргумент контроллера по типу `RequestContext`, **без** `@SapSystemParams` и **без** `@CurrentRequestContext`.
- Прикладной сервис и интеграционный адаптер — проверяют результат и переводят технические ошибки интеграции в типизированные исключения.
- Новый `@RestControllerAdvice` — **сохраняет аудит внутри обработчика ошибок**, формирует `ServiceErrorDto`, определяет HTTP-статус.
- `AuditSenderResolver` и стратегии `AuditSender` — выбирают отправителя через `supports(requestType)`; у каждой стратегии три метода для `UNEXPECTED`, `FORBIDDEN`, `NOT_FOUND`.

**Старые реализации не переписываем.** Новый advice ограничен пакетом новых контроллеров и имеет более высокий приоритет. Старый глобальный advice при этом формально остаётся применимым: раздельность обеспечиваем приоритетом, полным покрытием нужных исключений новым advice и тестами.

### Схема

```text
HTTP request
   │
   ├─ Spring Security (может завершить запрос до MVC)
   ▼
RequestContextInterceptor
   ├─ читает @RequestType из HandlerMethod
   ├─ читает заголовки
   └─ request.setAttribute(CONTEXT_ATTRIBUTE, RequestContext)
   ▼
RequestContextArgumentResolver
   └─ возвращает тот же RequestContext
   ▼
Controller → Application Service → Integration Adapter → SAP
                     │                 │
                     └──── typed exception ◀── HTTP/client failure
                                        │
                                        ▼
                              New @RestControllerAdvice
                                ├─ классифицирует ошибку
                                ├─ читает RequestContext
                                ├─ AuditSenderResolver.resolve(requestType)
                                │    └─ AuditSender (3 метода)
                                └─ HTTP status + ServiceErrorDto
```

## 3. Шаг 1 — RequestContext и аннотация операции

### Было

```java
setAttributes(request, "easup_payavg_event");
```

### Стало

```java
public record RequestContext(
        String requestType,
        String traceId,
        String sessionId,
        String pernr,
        String adLogin,
        String realm,
        String role,
        String rolesHeader
) {}
```

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestType {
    String value();
}
```

**Что изменяется:** контекст становится единым типизированным и immutable-объектом. `requestType` определяется по аннотации, а не по вызову служебного метода из тела контроллера.

`traceId` в этом варианте берётся из исходного `logId` для совместимости; в будущем его можно связать с OpenTelemetry. Значения `pernr`, `adLogin`, `role`, `Roles` и `realm` из заголовков допустимо принимать только в рамках существующей доверенной схемы (проверенный gateway/JWT); сами по себе заголовки не удостоверяют личность.

## 4. Шаг 2 — RequestContextInterceptor

### Было

Создание атрибутов выполнялось только после входа в контроллер.

### Стало

```java
@Component
public class RequestContextInterceptor implements HandlerInterceptor {

    public static final String CONTEXT_ATTRIBUTE =
            RequestContext.class.getName();

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequestType annotation = handlerMethod.getMethodAnnotation(RequestType.class);
        if (annotation == null) {
            return true; // старые эндпоинты не затрагиваем
        }

        RequestContext context = new RequestContext(
                annotation.value(),
                request.getHeader("logId"),
                request.getHeader("sessionId"),
                request.getHeader("pernr"),
                request.getHeader("X-user-login"),
                request.getHeader("realm"),
                request.getHeader("role"),
                request.getHeader("Roles")
        );
        request.setAttribute(CONTEXT_ATTRIBUTE, context);
        return true;
    }
}
```

**Что изменяется:** для контроллеров с `@RequestType` контекст создаётся до разрешения аргументов и до выполнения тела метода; для старых контроллеров interceptor ничего не делает.

**Ограничение:** `preHandle()` работает внутри Spring MVC. Ошибки, возникшие ранее (например, 401/403 из Spring Security), не получают этот контекст и обычно обрабатываются `AuthenticationEntryPoint`/`AccessDeniedHandler`, а не MVC `@ControllerAdvice`. Данный ADR не переносит аудит таких ошибок.

## 5. Шаг 3 — Resolver и MVC-конфигурация

### Было

Контроллер принимал `HttpServletRequest`, вручную читал заголовки и создавал `SapSystemParamsDto`.

### Стало

```java
@Component
public class RequestContextArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return RequestContext.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new IllegalStateException("HttpServletRequest is unavailable");
        }
        Object attribute = request.getAttribute(RequestContextInterceptor.CONTEXT_ATTRIBUTE);
        if (!(attribute instanceof RequestContext context)) {
            throw new IllegalStateException("RequestContext is not initialized");
        }
        return context;
    }
}
```

```java
@Configuration
@RequiredArgsConstructor
public class RequestContextMvcConfiguration implements WebMvcConfigurer {

    private final RequestContextInterceptor interceptor;
    private final RequestContextArgumentResolver argumentResolver;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(argumentResolver);
    }
}
```

**Что изменяется:** `RequestContext` создаётся **один раз**, `ArgumentResolver` только извлекает его. В новой реализации **нет `@SapSystemParams` и нет `@CurrentRequestContext`**.

## 6. Шаг 4 — новый контроллер и сервис

### Контроллер

```java
@RestController
@RequestMapping("/payavg") // пример префикса: сохранить фактический URL-контракт
@RequiredArgsConstructor
public class PayavgController {

    private final PayavgService payavgService;

    @RequestType("easup_payavg_event")
    @GetMapping(value = "/event", produces = MediaType.APPLICATION_JSON_VALUE)
    public PayavgEventDto getPayavgEvent(RequestContext context) {
        return payavgService.getPayavgEvent(context);
    }
}
```

> **Важно:** в исходном фрагменте был показан только метод `GET /event`, без class-level `@RequestMapping`. Префикс `/payavg` выше — **условный пример**, а не подтверждённый исходный URL. При переносе следует сохранить требуемый внешний маршрут.

### Сервис

```java
@Service
@RequiredArgsConstructor
public class PayavgService {
    private final PayavgIntegrationPort integrationPort;

    public PayavgEventDto getPayavgEvent(RequestContext context) {
        PayavgEventDto dto = integrationPort.getPayavgEvent(context);
        if (dto == null) {
            throw new PayavgEventNotFoundException();
        }
        return dto;
    }
}
```

```java
public interface PayavgIntegrationPort {
    PayavgEventDto getPayavgEvent(RequestContext context);
}
```

**Что изменяется:** контроллер не знает `HttpServletRequest`, не формирует HTTP-ошибку и не создаёт SAP DTO. Сервис фиксирует правило исходного поведения `null → not found`. Реализация порта будет вызывать **существующий клиент целевого сервиса**; его детали вне ADR. Если клиенту требуется `SapSystemParamsDto`, адаптер может сформировать его **из `RequestContext` на границе интеграции**, без возвращения `@SapSystemParams` в контроллер.

### Типизированные ошибки

```java
public class PayavgEventNotFoundException extends RuntimeException {
    public PayavgEventNotFoundException() {
        super("Данные события среднего заработка не найдены");
    }
}
```

```java
@Getter
public class ExternalSystemException extends RuntimeException {
    private final int upstreamStatus;

    public ExternalSystemException(int upstreamStatus, String message, Throwable cause) {
        super(message, cause);
        this.upstreamStatus = upstreamStatus;
    }
}
```

```java
public class ExternalSystemUnavailableException extends RuntimeException {
    public ExternalSystemUnavailableException(Throwable cause) {
        super("Внешняя система недоступна", cause);
    }
}
```

### Пример преобразования исключений на границе адаптера

```java
try {
    // вызов существующего HTTP-клиента целевого сервиса
} catch (HttpStatusCodeException e) {
    throw new ExternalSystemException(
            e.getStatusCode().value(), "Ошибка обращения к SAP", e);
} catch (ResourceAccessException e) {
    throw new ExternalSystemUnavailableException(e);
}
```

Это **псевдофрагмент границы адаптера**, не полная реализация метода. В реальной реализации необходимо отдельно учесть ошибку парсинга ответа, таймаут, статус 510 и корпоративный контракт ошибок. Не все 401 от SAP должны преобразовываться в 401 для конечного клиента.

## 7. Шаг 5 — новый ControllerAdvice, аудит остаётся внутри

### Было

Старый advice читал пять отдельных request attributes и вызывал крупные `switch` по `requestType`.

### Стало

Новый advice обрабатывает типизированные исключения, получает `RequestContext`, инициирует аудит и возвращает `ServiceErrorDto`. Отдельный аспект аудита не вводим.

```java
@RestControllerAdvice(basePackageClasses = PayavgController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class PayavgExceptionHandler {

    private final AuditSenderResolver auditSenderResolver;

    private RequestContext context(HttpServletRequest request) {
        Object value = request.getAttribute(RequestContextInterceptor.CONTEXT_ATTRIBUTE);
        return value instanceof RequestContext ctx ? ctx : null;
    }

    @ExceptionHandler(PayavgEventNotFoundException.class)
    public ResponseEntity<ServiceErrorDto> handleNotFound(
            PayavgEventNotFoundException ex, HttpServletRequest request) {
        RequestContext ctx = context(request);
        sendAudit(ctx, AuditErrorType.NOT_FOUND);
        return error(HttpStatus.NOT_FOUND, "404", ex.getMessage(), ctx);
    }

    @ExceptionHandler(ExternalSystemException.class)
    public ResponseEntity<ServiceErrorDto> handleExternal(
            ExternalSystemException ex, HttpServletRequest request) {
        RequestContext ctx = context(request);
        AuditErrorType auditType = switch (ex.getUpstreamStatus()) {
            case 401, 403 -> AuditErrorType.FORBIDDEN;
            case 404 -> AuditErrorType.NOT_FOUND;
            default -> AuditErrorType.UNEXPECTED;
        };
        sendAudit(ctx, auditType);
        // Пример политики: upstream 404 → клиенту 404, остальные → 502.
        // Финальную политику следует подтвердить API-контрактом.
        HttpStatus status = ex.getUpstreamStatus() == 404
                ? HttpStatus.NOT_FOUND : HttpStatus.BAD_GATEWAY;
        return error(status, String.valueOf(status.value()),
                "Ошибка внешней системы", ctx);
    }

    @ExceptionHandler(ExternalSystemUnavailableException.class)
    public ResponseEntity<ServiceErrorDto> handleUnavailable(
            ExternalSystemUnavailableException ex, HttpServletRequest request) {
        RequestContext ctx = context(request);
        sendAudit(ctx, AuditErrorType.UNEXPECTED);
        return error(HttpStatus.SERVICE_UNAVAILABLE, "503", ex.getMessage(), ctx);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ServiceErrorDto> handleUnexpected(
            Exception ex, HttpServletRequest request) {
        RequestContext ctx = context(request);
        log.error("Unexpected Payavg request failure", ex);
        sendAudit(ctx, AuditErrorType.UNEXPECTED);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "500",
                "Внутренняя ошибка сервиса", ctx);
    }

    private ResponseEntity<ServiceErrorDto> error(
            HttpStatus status, String code, String message, RequestContext ctx) {
        String traceId = ctx != null && ctx.traceId() != null
                ? ctx.traceId() : "undefined";
        ServiceErrorDto body = new ServiceErrorDto(
                new ServiceError(message, code, traceId, new ArrayList<>())
        );
        return ResponseEntity.status(status).body(body);
    }

    private void sendAudit(RequestContext ctx, AuditErrorType type) {
        if (ctx == null || ctx.requestType() == null) {
            log.warn("Audit skipped: request context is unavailable");
            return;
        }
        try {
            AuditSender sender = auditSenderResolver.resolve(ctx.requestType())
                    .orElseThrow(() -> new IllegalStateException(
                            "No AuditSender for " + ctx.requestType()));
            switch (type) {
                case UNEXPECTED -> sender.sendUnexpectedError(ctx);
                case FORBIDDEN -> sender.sendForbidden(ctx);
                case NOT_FOUND -> sender.sendNotFound(ctx);
            }
        } catch (Exception auditException) {
            log.error("Audit delivery failed for {}", ctx.requestType(), auditException);
            // Не подменяем исходную ошибку интеграции ошибкой аудита.
        }
    }
}
```

**Примечание по ошибкам валидации:** `@ExceptionHandler(Exception.class)` перехватит также многие неучтённые исключения MVC. До эксплуатации нужно добавить специализированные обработчики для валидируемых параметров и согласовать статусы. Иначе часть 400/422 может ошибочно превратиться в 500.

**Примечание по модели:** `ServiceErrorDto` / `ServiceError` приведены в виде, соответствующем показанному исходному коду. При переносе сверить реальные конструкторы и общие правила форматирования ошибки целевого сервиса.

### Совместная работа со старым advice

- Новый advice ограничивается `PayavgController` через `basePackageClasses`.
- Новый advice имеет `@Order(Ordered.HIGHEST_PRECEDENCE)`.
- Старый advice остаётся без изменений и глобально применимым; для нового контроллера новый advice перехватывает обработанные им исключения прежде старого.
- Не следует полагаться только на пакет без `@Order`: глобальный старый обработчик может выбрать исключение первым.
- Проверить тестами, что исключения нового контроллера не попадают в старый `ExceptionHandler`, а старые контроллеры продолжают использовать прежнюю схему.
- Ошибки Spring Security до MVC **не обрабатываются** этим advice.

## 8. Шаг 6 — аудит через AuditSenderResolver

### Интерфейс стратегии

```java
public interface AuditSender {
    boolean supports(String requestType);
    void sendUnexpectedError(RequestContext context);
    void sendForbidden(RequestContext context);
    void sendNotFound(RequestContext context);
}
```

```java
public enum AuditErrorType {
    UNEXPECTED, FORBIDDEN, NOT_FOUND
}
```

### Sender для переносимой операции

```java
@Component
@RequiredArgsConstructor
public class PayavgEventAuditSender implements AuditSender {

    private final AuditEventSenderCustom delegate;

    @Override
    public boolean supports(String requestType) {
        return "easup_payavg_event".equals(requestType);
    }

    @Override
    public void sendUnexpectedError(RequestContext context) {
        delegate.sendEasupPayavgEventError(
                LocalDateTime.now(), context.pernr(),
                context.sessionId(), context.adLogin());
    }

    @Override
    public void sendForbidden(RequestContext context) {
        delegate.sendEasupPayavgEventForbidden(
                LocalDateTime.now(), context.pernr(),
                context.sessionId(), context.adLogin());
    }

    @Override
    public void sendNotFound(RequestContext context) {
        delegate.sendEasupPayavgEventNotFound(
                LocalDateTime.now(), context.pernr(),
                context.sessionId(), context.adLogin());
    }
}
```

**Что изменяется:** три исторически разных события аудита остаются, но их выбор инкапсулирован в стратегии конкретной операции. Пример предполагает, что `AuditEventSenderCustom` доступен в целевом сервисе; если его там нет, стратегия делегирует в фактический локальный TSAU-сервис. Никакой необходимости переносить весь старый `AuditEventSenderCustom` и тысячи `case` нет.

### Resolver

```java
@Component
@RequiredArgsConstructor
public class AuditSenderResolver {

    private final List<AuditSender> senders;

    public Optional<AuditSender> resolve(String requestType) {
        List<AuditSender> matched = senders.stream()
                .filter(sender -> sender.supports(requestType))
                .toList();
        if (matched.size() > 1) {
            throw new IllegalStateException(
                    "Multiple AuditSenders for requestType=" + requestType);
        }
        return matched.stream().findFirst();
    }
}
```

В отличие от простого `findFirst()`, здесь **двойное совпадение не игнорируется**. Отсутствие sender также логируется через `sendAudit()`; в production следует мониторить такие ситуации, чтобы не терять обязательный аудит незаметно. По желанию можно валидировать известные идентификаторы операций при старте приложения.

### Вопрос о времени события

В старом обработчике использовалось `LocalDateTime.now()` в момент обработки ошибки. В примерах сохраняем этот подход. Если бизнес-контракт требует время **начала запроса**, его нужно отдельно добавить в `RequestContext` и передавать в sender; сейчас это не доказано предоставленным исходным кодом.

## 9. Контракт HTTP-ошибок: что сохраняем и что требует решения

| Сценарий | Исходное поведение | Предложение для нового обработчика | Статус решения |
|---|---|---|---|
| `null` после вызова интеграции | `HttpResponseException(404)` → 404 | `PayavgEventNotFoundException` → 404 | Сохраняем |
| Upstream 401/403 | В старом обработчике `HttpClientErrorException` обычно возвращал upstream status и отправлял Forbidden-аудит | Клиентский статус требует согласования; аудит `FORBIDDEN` | Открыто |
| Upstream 404 | Старый обработчик возвращал 404 и NotFound-аудит | 404 и `NOT_FOUND` | Проверить формат ошибки |
| Upstream 500 | Старый обработчик `HttpServerErrorException` обычно возвращал upstream status | Предложен 502 и `UNEXPECTED` | Изменение контракта, согласовать |
| Upstream 510 | В старом `HttpServerErrorException` аудит не отправлялся при 510 | Нужно отдельно сохранить исключение из аудита, если оно требуется | Открыто |
| Недоступность SAP | Возможное попадание в общий обработчик | Предложено 503 | Открыто |
| Таймаут SAP | Не выделен в показанной исходной реализации | Можно выделить 504 | Открыто |
| NPE | Старый обработчик возвращал 404 | 500 + `UNEXPECTED` | Намеренное исправление, проверить совместимость |
| Валидация до контроллера | Специальный обработчик в старом advice | Определить контракт нового advice | Открыто |

**Важно:** в документе есть примеры будущей архитектуры и отдельно отмечены изменения поведения, которые нельзя внедрять незаметно. Для миграции «без изменения контракта» сначала записать текущие результаты в тестах и принять явное решение по каждой спорной категории.

## 10. Тестирование

### Unit-тесты

1. `RequestContextArgumentResolver.supportsParameter()` принимает только `RequestContext`.
2. Resolver возвращает **тот же объект**, который сохранил interceptor; отсутствие объекта приводит к явной ошибке.
3. `AuditSenderResolver` выбирает единственный sender через `supports(requestType)`.
4. При отсутствии sender получаем `Optional.empty()`; при двух совпадениях — исключение.
5. Для `PayavgEventAuditSender` каждый из трёх методов вызывает ровно соответствующий метод TSAU-делегата.

### Интеграционные тесты MockMvc

1. Запрос к `/event` с заголовками `logId`, `sessionId`, `pernr`, `X-user-login`, `realm`, `role`, `Roles` попадает в сервис с корректным `RequestContext`.
2. Контекст содержит `requestType="easup_payavg_event"`.
3. Успешный ответ сериализует `PayavgEventDto` и возвращает 200.
4. `null` от интеграции приводит к 404 + согласованному `ServiceErrorDto` и `sendNotFound()`.
5. Upstream 401/403 вызывает `sendForbidden()`; HTTP-статус соответствует согласованной политике.
6. Upstream 500 вызывает `sendUnexpectedError()`; HTTP-статус соответствует согласованной политике.
7. Неожиданное исключение вызывает `sendUnexpectedError()` и безопасный 500.
8. Сбой отправки аудита **не изменяет** HTTP-ответ по исходному исключению.
9. Существующий старый контроллер без `@RequestType` продолжает обрабатываться старым advice.
10. Исключения нового контроллера не перехватываются старым глобальным advice.
11. Ранние ошибки (валидация/argument resolution) проверяются отдельно, включая отсутствие контекста.

### WireMock

Настроить ответы SAP на 200, 401, 403, 404, 500, таймаут, невалидный JSON и пустое тело. Проверить преобразование их интеграционным адаптером, но **не переопределять настройки локального EasupClient в рамках этого ADR**.

Пример идеи проверки аудита:

```java
verify(payavgEventAuditSender).sendNotFound(
        argThat(ctx -> "easup_payavg_event".equals(ctx.requestType())));
```

Для `MockMvc` убедиться, что в тестовом контексте зарегистрированы interceptor и resolver.

## 11. План внедрения

1. Добавить `RequestContext` и `@RequestType` без изменения существующих контроллеров.
2. Зарегистрировать `RequestContextInterceptor` и `RequestContextArgumentResolver`.
3. Написать unit/MockMvc-тесты формирования и разрешения контекста.
4. Перенести контроллер `GET /event` в новый пакет, сохранив фактический URI, метод и формат ответа.
5. Добавить `PayavgService`, порт интеграции и типизированные исключения; подключение клиента — отдельная задача.
6. Добавить новый scoped `@RestControllerAdvice`, сохранить аудит **внутри него**.
7. Добавить `AuditSender`, `AuditSenderResolver` и реализацию для `easup_payavg_event`.
8. Согласовать и зафиксировать таблицу HTTP-статусов / кодов ошибок / обязательных событий аудита.
9. Прогнать интеграционные тесты и проверить отсутствие регрессий старых контроллеров.
10. Переключить маршрут на новый сервис после проверки конфигурации и интеграций.

## 12. Последствия и компромиссы

**Преимущества:**

- Нет `setSystemParams()`/`setAttributes()` в новых контроллерах.
- Один immutable-контекст создаётся перед контроллером и доступен в MVC advice.
- Нет зависимости нового контроллера от `HttpServletRequest`, `@SapSystemParams` или `@CurrentRequestContext`.
- Типизированные исключения вместо разбора сообщений ошибок.
- Аудит остаётся в `ControllerAdvice` по принятому проектному решению.
- Новый sender добавляется без изменения центральных `switch`.
- Старый подход может продолжать существовать параллельно.

**Ограничения / риски:**

- MVC interceptor не видит события, отклонённые до MVC (Security, некоторые фильтры).
- `RequestContext` сейчас содержит поля интеграции; в дальнейшем возможно разделение общего контекста и адаптерских параметров.
- Новый глобальный fallback `Exception.class` требует аккуратной маршрутизации и отдельных правил ошибок валидации.
- Несогласованная смена downstream→client HTTP-статусов может сломать потребителей.
- Отправка аудита в `ControllerAdvice` распространяется только на ошибки, прошедшие через соответствующий advice. Успешные события и pre-MVC security events требуют отдельного механизма, если обязательны.
- Поскольку аудит выполняется синхронно при обработке ошибки, его задержки могут влиять на latency, даже если ошибки sender перехватываются.

## 13. Критерии готовности

- [ ] Новый `GET /event` не использует `@SapSystemParams`, `@CurrentRequestContext`, `setSystemParams()` или `setAttributes()`.
- [ ] `RequestContext` создаётся только для методов с `@RequestType`.
- [ ] Новый advice возвращает согласованный `ServiceErrorDto`.
- [ ] Для `easup_payavg_event` отправляются верные `UNEXPECTED`/`FORBIDDEN`/`NOT_FOUND` события.
- [ ] Нет неоднозначного выбора AuditSender.
- [ ] Ошибка аудита не подменяет исходную ошибку API.
- [ ] Старые эндпоинты и их advice работают без изменения поведения.
- [ ] HTTP-статусы и формат ошибок зафиксированы и покрыты тестами.
- [ ] Клиент целевого сервиса подключён отдельным изменением и не дублируется.

## 14. Открытые вопросы

1. Какой фактический внешний URL переносимого `GET /event`, включая префикс контроллера?
2. Нужно ли строго сохранять исходные HTTP-статусы для ошибок SAP 401/403/500/510 или можно улучшить их маппинг?
3. Какие поля и форматы `ServiceErrorDto` обязательны для потребителей (код, details, logId)?
4. Доступны ли три метода аудита в целевом сервисе и совпадает ли их формат событий с исходным?
5. Нужно ли сохранять аудит 401/403 от Spring Security, которые не доходят до MVC?
6. Нужны ли ошибки парсинга SAP в отдельной категории аудита или достаточно `UNEXPECTED`?

---

**Решение на текущем этапе:** использовать `RequestContext` + `@RequestType` + MVC interceptor/resolver без `@SapSystemParams`; сохранить аудит внутри нового `ControllerAdvice`, выбирая стратегию по `AuditSenderResolver.supports(requestType)`; старую реализацию не изменять. Финальная политика HTTP-статусов и конфигурация интеграционного клиента требуют отдельного согласования.
