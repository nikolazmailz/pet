# ADR-003. Обработка ошибок и аудит интеграционных REST-операций EASUP

**Статус:** Proposed  
**Область:** перенос интеграционных REST-операций EASUP в целевой микросервис  
**Пример операции:** `GET /event` (`easup_payavg_event`)  
**Технологии:** Java 17, Spring Boot 3.x, Spring MVC, TSAU  
**Ответственные:** команда разработки сервиса (назначить при принятии ADR)

## 1. Контекст

При переносе интеграционного эндпоинта EASUP необходимо сохранить контракт HTTP-ответов, необходимые данные для исходящего запроса и события аудита, одновременно сократив связанность контроллера с обработкой исключений.

Текущий контроллер (сокращённый пример из исходной реализации):

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

`setSystemParams()` извлекает заголовки `pernr`, `sessionId`, `role`, `Roles`, `logId`, `realm` и формирует `SapSystemParamsDto`, передаваемый дальше в интеграционный сервис.

`setAttributes()` отдельно записывает `requestType`, `sessionId`, `pernr`, `adlogin`, `traceId` в `HttpServletRequest`. Эти атрибуты затем считываются в `EasupIntegrationExceptionHandler`, где выбираются методы аудита по строковому `requestType`.

Сервис формирует URL, вызывает HTTP-клиент и преобразует тело ответа в `PayavgEventDto`. **Адаптация HTTP-клиента целевого микросервиса находится вне области этого ADR.**

### Проблемы текущего подхода

1. **Ручная подготовка аудита в контроллере.** Пропуск `setAttributes()` приводит к отсутствию части данных в обработчике ошибок.
2. **Дублирование источников данных.** Одни и те же заголовки читаются для `SapSystemParamsDto` и отдельно для атрибутов запроса.
3. **Обязанности смешаны.** Контроллер формирует интеграционные параметры и принимает решение об HTTP-ошибке; `ControllerAdvice` одновременно строит ответ, читает атрибуты, логирует и выбирает событие аудита.
4. **Неустойчивое распознавание ошибок.** Логика вида `e.getMessage().contains("SAP")` зависит от текста исключения и может сама вызвать ошибку при `null`.
5. **Большие `switch` по строкам операций.** Для каждого класса ошибок поддерживаются отдельные ветвления по `requestType`; возможны пропуски и ошибки `break`.
6. **Некорректное обобщение исключений.** Например, произвольный `NullPointerException` преобразуется в HTTP 404, хотя обычно означает технический дефект.
7. **Потеря контекста для ранних ошибок.** Пока управление не дошло до контроллера и записи атрибутов, эти атрибуты отсутствуют.

## 2. Решение

Принять для переносимых интеграционных операций следующую структуру:

- **`SapSystemParamsDto`** сохраняется как привычный параметр интеграционного сценария. Его создаёт уже используемый механизм `@SapSystemParams` / `HandlerMethodArgumentResolver`.
- **`@RequestType`** задаёт идентификатор операции непосредственно в метаданных метода контроллера.
- **Контроллер** только вызывает сервис и возвращает типизированный ответ.
- **Сервис и интеграционная граница** преобразуют ожидаемые результаты и технические сбои в типизированные исключения.
- **Новый `@RestControllerAdvice`** формирует `ServiceErrorDto`, журналирует ошибки и **инициирует аудит**. Для получения идентификатора операции используется информация о сопоставленном методе контроллера; необходимые пользовательские значения читаются из текущего запроса или доверенного security-контекста.
- **`AuditSenderResolver`** выбирает стратегию по `supports(requestType)`.
- **Отдельный `AuditSender`** для каждой операции реализует три метода: `sendUnexpectedError`, `sendForbidden`, `sendNotFound`.
- **Существующие эндпоинты и их обработка ошибок остаются без изменений.** Новый обработчик ограничивается новыми контроллерами и имеет приоритет над глобальным обработчиком для запросов этих контроллеров.

### Схема

```text
HTTP-запрос
    |
    v
Spring MVC: сопоставление метода контроллера
    |
    +--> @SapSystemParams / HandlerMethodArgumentResolver
    |         |
    |         v
    |     SapSystemParamsDto
    |         |
    v         v
@RequestType + Controller --> Service --> интеграционная граница --> EASUP
                                     |
                                     | исключение
                                     v
                           Новый RestControllerAdvice
                             |              |
                             |              +--> ServiceErrorDto / HTTP
                             v
                      AuditSenderResolver
                        supports(requestType)
                             |
                             v
                       AuditSender (3 метода)
                             |
                             v
                    AuditEventSenderCustom / TSAU
```

## 3. Границы решения

**Входит:** контроллер переносимой операции, `@RequestType`, типизированные исключения, новый `ControllerAdvice`, `AuditSender` и `AuditSenderResolver`, тесты, проверка соответствия старому контракту.

**Не входит:** замена или переустройство целевого `EasupClient`, изменения глобальных правил аутентификации, изменение схемы событий TSAU, массовый рефакторинг действующих эндпоинтов, глобальная смена формата `ServiceErrorDto`.

## 4. Аннотация операции

Идентификатор операции определяется на методе контроллера.

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestType {
    String value();
}
```

Использование:

```java
@RequestType("easup_payavg_event")
@GetMapping(value = "/event", produces = MediaType.APPLICATION_JSON_VALUE)
public PayavgEventDto getPayavgEvent(
        @SapSystemParams SapSystemParamsDto systemParams
) {
    return payavgService.getPayavgEvent(systemParams);
}
```

Здесь нет чтения `HttpServletRequest`, ручной записи атрибутов и конструирования HTTP-исключения.

**Почему аннотация сохранена у `SapSystemParamsDto`:** существующий `HandlerMethodArgumentResolver` явно распознаёт нужный аргумент, не конфликтуя со стандартным связыванием DTO как `@ModelAttribute`. В контракте сервисного метода `SapSystemParamsDto` остаётся без изменений.

## 5. Определение операции в ControllerAdvice

После успешного сопоставления HTTP-запроса с методом Spring MVC помещает информацию о методе в атрибут запроса `HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE`.

```java
private Optional<String> resolveRequestType(HttpServletRequest request) {
    Object handler = request.getAttribute(
            HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);

    if (!(handler instanceof HandlerMethod handlerMethod)) {
        return Optional.empty();
    }

    RequestType annotation = handlerMethod.getMethodAnnotation(RequestType.class);
    return annotation == null
            ? Optional.empty()
            : Optional.of(annotation.value());
}
```

Не следует делать небезопасный cast к `HandlerMethod` или предполагать обязательное наличие аннотации.

**Ограничение:** если ошибка возникает до сопоставления метода (например, отказ Spring Security в filter chain, ошибка маршрутизации или часть ошибок в инфраструктуре), определить операцию таким способом нельзя. Подобные ошибки обрабатываются своей инфраструктурной цепочкой; для них при необходимости задаётся отдельная политика аудита. Если метод уже сопоставлен, но разрешение аргументов не удалось, атрибут обработчика обычно уже доступен.

## 6. Источники данных для аудита

В текущем контракте отправки события используются `pernr`, `sessionId`, `adlogin` и время события; для ошибки/логов также нужен `traceId`.

| Значение | Источник в текущем протоколе | Условия |
|---|---|---|
| `requestType` | `@RequestType` выбранного `HandlerMethod` | Может отсутствовать при ранней ошибке |
| `pernr` | Заголовок `pernr` | Сверить с доверенным источником идентичности |
| `sessionId` | Заголовок `sessionId` | При отсутствии — явная политика |
| `adlogin` | Заголовок `X-user-login` | Не считать произвольный заголовок аутентифицированной идентичностью |
| `traceId` | Заголовок `logId` / существующий tracing | Предпочесть авторитетный tracing-контекст, если он уже настроен |

Для уменьшения дублирования удобно выделить **внутренний метод чтения полей аудита в новом обработчике или отдельный `AuditMetadataExtractor`**. Это локальная реализационная деталь; новый общий контракт между бизнес-сервисами не требуется.

Например, вызов непосредственно в advice:

```java
String pernr = request.getHeader("pernr");
String sessionId = request.getHeader("sessionId");
String adLogin = request.getHeader("X-user-login");
String traceId = request.getHeader("logId");
```

Чтение только в обработчике исключения достаточно для ошибок, дошедших до MVC. Необходимо заранее определить, что делать при отсутствии полей: логировать нарушение контракта, не подменять исходную ошибку и не передавать заведомо ложные идентификаторы в TSAU.

## 7. Разделение ошибок

### Ошибки приложения и интеграции

- `PayavgEventNotFoundException` — отсутствие ожидаемого результата операции (в исходном контроллере `null` приводил к 404).
- `ExternalSystemException` — внешний сервис ответил с ошибкой; хранит статус **внешнего** ответа и исходную причину.
- `ExternalSystemUnavailableException` — ошибка соединения/доступности внешней системы.
- Для таймаута при необходимости выделяется отдельный тип.

Пример модели интеграционной ошибки:

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

**Не требуется** заворачивать все исключения в доменные. NPE и ошибки программирования остаются неожиданными техническими ошибками с ответом 500; они не должны маскироваться под «не найдено».

### Где выполняется преобразование

На границе HTTP-интеграции (псевдофрагмент; конкретный клиент целевого сервиса — отдельная задача):

```java
catch (HttpStatusCodeException ex) {
    throw new ExternalSystemException(
            ex.getStatusCode().value(), "Ошибка ответа EASUP", ex);
}
```

Сервис проверяет бизнес-смысл результата:

```java
PayavgEventDto result = integrationPort.getPayavgEvent(systemParams);
if (result == null) {
    throw new PayavgEventNotFoundException();
}
return result;
```

`integrationPort` — условная абстракция; при переносе можно оставить существующий интерфейс сервиса. Не нужно вводить новый слой исключительно ради выполнения этого ADR.

## 8. Политика HTTP-ответов и аудита

Разделяем: **HTTP-статус внешней системы**, **HTTP-статус нашего ответа** и **категорию аудита**. Они не всегда совпадают.

| Ситуация | Рекомендуемый ответ | Категория аудита | Замечание |
|---|---:|---|---|
| Вызов выполнен успешно | 200 | Здесь не рассматривается | Возвращается `PayavgEventDto` |
| Данные отсутствуют (`null`) | 404 | `NOT_FOUND` | Сохранить исходный контракт |
| EASUP вернул 404 | Уточнить контракт | `NOT_FOUND` при соответствующем смысле | Не всякий внешний 404 означает отсутствие бизнес-данных |
| EASUP вернул 401/403 | Уточнить контракт | `FORBIDDEN` при согласованной семантике | Не обязательно возвращать 401/403 вызывающему клиенту |
| EASUP вернул 5xx | Обычно 502 | `UNEXPECTED` | При миграции сверить с текущим API |
| EASUP недоступен | Обычно 503 | `UNEXPECTED` | Отдельная ошибка соединения |
| Таймаут EASUP | Обычно 504 | `UNEXPECTED` | Различать с иными ошибками доступа |
| Непредвиденная ошибка приложения | 500 | `UNEXPECTED` | Не преобразовывать NPE в 404 |
| Внешний статус 510 | Сохранить действующее правило до согласования | Уточнить | В существующем обработчике отдельное исключение из аудита |

**Правило миграции:** прежде чем менять внешний HTTP-статус или тело `ServiceErrorDto`, зафиксировать действующий контракт тестами и согласовать отклонения. Нельзя незаметно заменять исходные 401/403/404/500 на новые статусы.

## 9. Контракт AuditSender

Один отправитель на одну операцию, три метода на категории ошибок:

```java
public interface AuditSender {
    boolean supports(String requestType);

    void sendUnexpectedError(AuditEventData data);
    void sendForbidden(AuditEventData data);
    void sendNotFound(AuditEventData data);
}
```

`AuditEventData` — локальный объект аргументов события, **не контекст HTTP-запроса**. Он не передаётся через сервисы и нужен лишь для устранения длинных списков параметров:

```java
public record AuditEventData(
        LocalDateTime occurredAt,
        String pernr,
        String sessionId,
        String adLogin
) {}
```

Если команде удобнее существующие четыре аргумента методов, можно не вводить `AuditEventData` и оставить `(LocalDateTime, String, String, String)` в контракте. Выбранную форму нужно использовать единообразно.

### Отправитель для `easup_payavg_event`

```java
@Component
@RequiredArgsConstructor
public class PayavgEventAuditSender implements AuditSender {
    private static final String OPERATION = "easup_payavg_event";

    private final AuditEventSenderCustom delegate;

    @Override
    public boolean supports(String requestType) {
        return OPERATION.equals(requestType);
    }

    @Override
    public void sendUnexpectedError(AuditEventData data) {
        delegate.sendEasupPayavgEventError(
                data.occurredAt(), data.pernr(), data.sessionId(), data.adLogin());
    }

    @Override
    public void sendForbidden(AuditEventData data) {
        delegate.sendEasupPayavgEventForbidden(
                data.occurredAt(), data.pernr(), data.sessionId(), data.adLogin());
    }

    @Override
    public void sendNotFound(AuditEventData data) {
        delegate.sendEasupPayavgEventNotFound(
                data.occurredAt(), data.pernr(), data.sessionId(), data.adLogin());
    }
}
```

Названия методов делегата соответствуют обнаруженным в исходном обработчике; реальные сигнатуры и необходимость `throws` уточняются по коду `AuditEventSenderCustom`.

## 10. AuditSenderResolver

Spring внедряет все зарегистрированные реализации `AuditSender`. Resolver вызывает `supports(requestType)` и должен обнаруживать неоднозначность:

```java
@Component
@RequiredArgsConstructor
public class AuditSenderResolver {
    private final List<AuditSender> senders;

    public Optional<AuditSender> resolve(String requestType) {
        if (requestType == null) {
            return Optional.empty();
        }

        List<AuditSender> matches = senders.stream()
                .filter(sender -> sender.supports(requestType))
                .toList();

        if (matches.size() > 1) {
            throw new IllegalStateException(
                    "Multiple AuditSender implementations for: " + requestType);
        }

        return matches.stream().findFirst();
    }
}
```

**Политика:** нет подходящего отправителя — предупреждение с идентификатором операции; найдено несколько — ошибка конфигурации с диагностикой. В обоих случаях исходная HTTP-ошибка не должна подменяться сбоем механизма аудита.

## 11. Новый RestControllerAdvice

Новый advice применяется только к пакетам переносимых контроллеров; старый глобальный advice остаётся для действующих методов:

```java
@RestControllerAdvice(basePackageClasses = PayavgController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class IntegrationExceptionHandler {
    private final AuditSenderResolver auditSenderResolver;
    // ...
}
```

**Уточнение по `basePackageClasses`:** ограничение распространяется на **весь пакет** `PayavgController`, не на один класс. Если нужен охват только выбранных контроллеров, используйте `assignableTypes` или специальную marker-аннотацию. Не помещайте старые и новые контроллеры в один пакет без понимания этой границы.

Старый глобальный обработчик `@ExceptionHandler(Exception.class)` остаётся применим и к новым контроллерам. Чтобы приоритетно сработал новый, у него должны быть выше приоритет (`@Order`) и подходящие методы для всех ожидаемых категорий, включая fallback. Это следует проверить тестами. Ошибки вне MVC (в Spring Security/filter chain) данным решением не покрываются.

### Пример: 404 + аудит

```java
@ExceptionHandler(PayavgEventNotFoundException.class)
public ResponseEntity<ServiceErrorDto> handleNotFound(
        PayavgEventNotFoundException ex,
        HttpServletRequest request
) {
    sendAuditSafely(request, AuditErrorType.NOT_FOUND);
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(createError(ex.getMessage(), "404", request));
}
```

`createError()` конструирует **действующий** `ServiceErrorDto` и подставляет `traceId` из доверенного источника (с учётом принятого контракта). `sendAuditSafely()` определяет `requestType`, извлекает параметры события, выбирает отправителя и вызывает нужный метод. Ниже — центральная идея, без повторения всех обработчиков:

```java
private void sendAuditSafely(HttpServletRequest request, AuditErrorType type) {
    try {
        Optional<String> operation = resolveRequestType(request);
        if (operation.isEmpty()) {
            log.warn("No mapped operation for audit");
            return;
        }

        Optional<AuditSender> sender = auditSenderResolver.resolve(operation.get());
        if (sender.isEmpty()) {
            log.warn("No AuditSender for operation {}", operation.get());
            return;
        }

        AuditEventData data = extractAuditEventData(request);
        switch (type) {
            case UNEXPECTED -> sender.get().sendUnexpectedError(data);
            case FORBIDDEN -> sender.get().sendForbidden(data);
            case NOT_FOUND -> sender.get().sendNotFound(data);
        }
    } catch (Exception auditFailure) {
        log.error("Audit delivery failed", auditFailure);
    }
}
```

`extractAuditEventData()` читает указанные выше доверенные данные и фиксирует время отправляемого события. В зависимости от контракта TSAU потребуется политика пропущенных обязательных полей.

### Обработчики, которые должны присутствовать

- `PayavgEventNotFoundException` → 404, категория `NOT_FOUND`.
- `ExternalSystemException` → сопоставление `upstreamStatus` со статусом ответа и категорией аудита.
- `ExternalSystemUnavailableException` → согласованный статус, `UNEXPECTED`.
- Ошибки валидации → согласованное тело/статус; решение об отправке аудита отдельно.
- `Exception` → безопасное сообщение 500, `UNEXPECTED`, логирование stack trace.

Проверку `e.getMessage().contains("SAP")` исключить. Если необходимо анализировать стандартный ответ SAP, использовать структурированный парсер интеграционного формата на интеграционной границе, сохраняя корректную обработку некорректного тела.

**Аудит остаётся в `ControllerAdvice`**: именно он определяет категорию ошибки и вызывает resolver. Отправители только делегируют вызовы в действующий механизм TSAU.

## 12. Порядок внедрения

1. **Зафиксировать поведение текущего `GET /event`.** Успех, `null`, ответы EASUP 401/403/404/500/510, техническая ошибка, ошибки JSON; сравнить статус, тело и аудит.
2. **Подготовить пакет переносимого контроллера** и область действия нового `ControllerAdvice` так, чтобы не изменять старые эндпоинты.
3. **Создать `@RequestType`** и добавить идентификатор операции на метод контроллера.
4. **Сохранить `@SapSystemParams SapSystemParamsDto`** и существующий механизм разрешения аргумента; проверить корректность заголовков.
5. **Перенести вызов на сервисный уровень**, оставить отдельную интеграционную границу клиента; добавить типизированные исключения и правило `null` → 404.
6. **Реализовать `AuditSender` и `AuditSenderResolver`** с обнаружением конфликтов.
7. **Добавить новый `ControllerAdvice`**, включая извлечение `requestType` из сопоставленного метода и отправку аудита.
8. **Протестировать совместную работу двух advice**, включая общие и специализированные исключения, отсутствие sender и сбой TSAU.
9. **Включить переносимый эндпоинт** после подтверждения сопоставимости HTTP-контракта и ожидаемых событий аудита.

## 13. Тестирование и критерии приёмки

### Unit-тесты

- `AuditSenderResolver`: 0/1/2 подходящих sender, `null` requestType.
- `PayavgEventAuditSender`: корректный делегирующий вызов для всех трёх категорий.
- Сопоставление исключения с категорией аудита и HTTP-статусом.
- При исключении от TSAU исходный ответ об ошибке сохраняется.

### MVC / интеграционные тесты

- `MockMvc` подтверждает разрешение `@SapSystemParams` и получение заголовков.
- `MockMvc` подтверждает определение `@RequestType` через сопоставленный `HandlerMethod`.
- `GET /event` возвращает DTO с HTTP 200 при успешной интеграции.
- Ответ `null` преобразуется в согласованный `ServiceErrorDto` / HTTP 404 и вызывает `sendNotFound` один раз.
- Ошибки EASUP 401/403/404/500 и исключение 510 соответствуют согласованной таблице ответов и событий.
- При неожиданной ошибке приложения не возвращается ошибочный HTTP 404.
- Старый контроллер продолжает использовать старый advice, новый — новый advice; никаких двойных событий аудита.
- Отсутствие метаданных операции, обязательных заголовков или подходящего отправителя не приводит к случайному HTTP 500 из механизма аудита и обязательно диагностируется.
- Отдельным тестом подтверждается поведение при отказе в Spring Security до MVC (вне ответственности advice).

Тестирование EASUP через WireMock допустимо при наличии установленного клиентского адаптера. Его детальная настройка не является частью данного ADR.

### Критерии завершения

- Контроллер не вызывает `setAttributes()`/`setSystemParams()`.
- Входным параметром является `@SapSystemParams SapSystemParamsDto`.
- Для новой операции создан один sender с тремя методами, выбираемый через `supports(requestType)`.
- HTTP-ответы и аудит соответствуют согласованному контракту.
- Существующие эндпоинты не меняют поведение.
- Ошибка доставки аудита не подменяет исходную ошибку API и фиксируется в логах/мониторинге.

## 14. Риски и компромиссы

| Риск | Митигирующее действие |
|---|---|
| Не определён `HandlerMethod` | Безопасный fallback; отдельная обработка ошибок до MVC |
| Глобальный старый advice перехватывает новый контроллер | Область действия + `@Order` + тесты конкурирующих обработчиков |
| Два sender поддерживают одну операцию | Явная проверка конфликта в resolver |
| Изменился внешний HTTP-контракт | Контрактные тесты, согласование таблицы преобразований |
| Аудит отсутствует или TSAU недоступен | Логи/метрики и контролируемый fallback, не подменяющий HTTP-ответ |
| Заголовок пользователя недостоверен | Проверять доверенный upstream/security-контекст; не использовать непроверенные заголовки для полномочий |
| Дублируются аудит-события из нового и существующего механизма | Тесты на ровно одну отправку и проверка интеграции с TSAU |

## 15. Альтернативы, рассмотренные при проектировании

- **Оставить текущую реализацию без изменений:** минимальные затраты на перенос, но сохраняются дублирование, ручная подготовка атрибутов, разросшийся обработчик и риск потери аудита.
- **Копировать старый `ControllerAdvice` целиком:** увеличивает стоимость сопровождения и риск расхождения реализаций.
- **Регистрировать отдельный обработчик каждого endpoint:** локально просто, но повторяет логику HTTP-ошибок и аудита.

Выбранный вариант позволяет выполнить локальную миграцию без массового изменения действующих REST-сервисов и параметров интеграции.

## 16. Открытые вопросы перед статусом Accepted

1. Какая точная матрица HTTP-статусов/тела `ServiceErrorDto` должна сохраняться для внешних 401/403/404/500/510?
2. Какие поля аудита обязательны и какой источник считается доверенным для `pernr`/`adlogin`?
3. Какое поведение нужно при отсутствии `requestType` или подходящего sender: только мониторинг или отдельная эксплуатационная тревога?
4. Как совмещаются имеющиеся события TSAU с перенесёнными `send*`-методами, чтобы избежать дублирования?
5. Какой точный пакет/marker-аннотация определяет новые контроллеры в целевом сервисе?

**Решение считается принятым после согласования открытых вопросов, реализации тестов и подтверждения неизменности действующих эндпоинтов.**
