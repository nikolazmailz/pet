## Задача 4. Перенести обработку результатов и выбрасывание доменных исключений в Use Case

### Название

**Перенести логику проверки результата операции и формирования доменных исключений из контроллеров в соответствующие Use Case**

### Цель

Убрать из REST-контроллеров логику интерпретации результатов выполнения операций и формирования исключений.

Каждый Use Case должен:

* выполнять соответствующий прикладной сценарий;
* анализировать результат вызова внешней системы или репозитория;
* определять успешность выполнения операции;
* преобразовывать ошибочные результаты в исключения из иерархии `DomainException`;
* возвращать контроллеру только успешный результат.

Контроллер должен отвечать только за HTTP-взаимодействие:

* принять запрос;
* получить системные параметры;
* вызвать Use Case;
* вернуть успешный DTO.

---

### Текущее состояние

Контроллер самостоятельно анализирует результат вызова сервиса:

```java
RequisitionGeneralCandidateDto requisitionResponse =
        requisitionService.getRequisitionPostAddComment(
                systemParams,
                requestDto
        );

if (requisitionResponse != null) {
    if (requisitionResponse.getResponseCode().startsWith("2")) {
        RequisitionGeneralCandidateDtoOut responseOut =
                new RequisitionGeneralCandidateDtoOut();

        BeanUtils.copyProperties(
                requisitionResponse,
                responseOut
        );

        return ResponseEntity.ok(responseOut);
    }

    int httpStatusNumber = Integer.parseInt(
            requisitionResponse
                    .getResponseCode()
                    .substring(0, 3)
    );

    throw new HttpResponseException(
            httpStatusNumber,
            requisitionResponse.getErrorMessage()
    );
}

throw new HttpResponseException(
        404,
        ErrorResponse.REQUISITION_INFO_NOT_FOUND
);
```

В результате контроллер:

* знает структуру ответа внешней системы;
* интерпретирует строковый `responseCode`;
* определяет тип ошибки;
* выбирает HTTP-статус;
* выбрасывает исключения;
* выполняет преобразование интеграционного DTO в API DTO;
* содержит несколько веток выполнения.

---

### Целевое состояние

Для каждого вызова контроллера должен существовать соответствующий Use Case.

Для операции добавления комментария:

```java
public interface AddRequisitionCommentUseCase {

    RequisitionGeneralCandidateDtoOut execute(
            SapSystemParamsDto systemParams,
            RequisitionAddCommentRequest request
    );
}
```

Реализация Use Case должна содержать всю прикладную логику сценария:

```java
@Service
@RequiredArgsConstructor
public class AddRequisitionCommentUseCaseImpl
        implements AddRequisitionCommentUseCase {

    private final RequisitionGateway requisitionGateway;
    private final RequisitionDtoMapper requisitionDtoMapper;

    @Override
    public RequisitionGeneralCandidateDtoOut execute(
            SapSystemParamsDto systemParams,
            RequisitionAddCommentRequest request
    ) {
        RequisitionGeneralCandidateDto response =
                requisitionGateway.addComment(
                        systemParams,
                        request
                );

        validateResponse(response);

        return requisitionDtoMapper.toOut(response);
    }

    private void validateResponse(
            RequisitionGeneralCandidateDto response
    ) {
        if (response == null) {
            throw new ExternalSystemUnavailableException(
                    "Внешняя система не вернула ответ",
                    null
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
    }

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

        int statusCode;

        try {
            statusCode = Integer.parseInt(
                    responseCode.substring(0, 3)
            );
        } catch (NumberFormatException ex) {
            throw new InvalidExternalSystemResponseException(
                    "Не удалось определить статус ответа внешней системы",
                    traceId,
                    ex
            );
        }

        try {
            return HttpStatus.valueOf(statusCode);
        } catch (IllegalArgumentException ex) {
            throw new InvalidExternalSystemResponseException(
                    "Внешняя система вернула неизвестный HTTP-статус: "
                            + statusCode,
                    traceId,
                    ex
            );
        }
    }
}
```

---

### Ответственность Use Case

Use Case должен принимать решение, что означает полученный результат для текущего прикладного сценария.

Например:

```text
null
    → ExternalSystemUnavailableException

некорректный responseCode
    → InvalidExternalSystemResponseException

таймаут внешней системы
    → ExternalSystemTimeoutException

заявка не найдена
    → RequisitionNotFoundException

операция запрещена текущим статусом заявки
    → InvalidRequisitionStateException

внешняя система вернула техническую ошибку
    → ExternalSystemResponseException
```

Use Case должен выбрасывать исключение с понятной семантикой, а не универсальное исключение с произвольным статусом.

---

### Разделение ответственности между слоями

#### Контроллер

Контроллер:

* принимает HTTP-запрос;
* выполняет первичную валидацию через `@Valid`;
* получает `SapSystemParamsDto` через `HandlerMethodArgumentResolver`;
* вызывает Use Case;
* возвращает успешный результат.

Контроллер не должен:

* проверять результат на `null`;
* анализировать `responseCode`;
* преобразовывать строковый код в HTTP-статус;
* выбрасывать доменные исключения на основании ответа внешней системы;
* выполнять интеграционное логирование;
* маппить интеграционные DTO в DTO контроллера.

#### Use Case

Use Case:

* управляет прикладным сценарием;
* вызывает необходимые порты и адаптеры;
* анализирует результат выполнения;
* применяет бизнес-правила;
* преобразовывает ожидаемые ошибочные результаты в `DomainException`;
* формирует успешный результат сценария.

#### Интеграционный адаптер

Интеграционный адаптер:

* выполняет технический вызов внешней системы;
* сериализует и десериализует сообщения;
* работает с Redis, Kafka, HTTP или другим транспортом;
* может выбрасывать низкоуровневые технические исключения.

Use Case должен при необходимости преобразовать низкоуровневое исключение адаптера в исключение из доменной иерархии:

```java
try {
    return requisitionGateway.addComment(
            systemParams,
            request
    );
} catch (RedisRequestTimeoutException ex) {
    throw new ExternalSystemTimeoutException(
            "Истекло время ожидания ответа ЕАСУП",
            TraceIdProvider.currentTraceId(),
            ex
    );
}
```

Низкоуровневые исключения Redis, HTTP-клиента или Kafka не должны напрямую попадать в контроллер и клиентский ответ.

#### `DomainExceptionHandler`

`DomainExceptionHandler`:

* перехватывает `DomainException`;
* определяет уровень логирования;
* формирует `ApiError`;
* возвращает HTTP-статус, содержащийся в исключении.

Advice не должен:

* определять бизнес-смысл ошибки;
* анализировать ответы внешних систем;
* заменять один тип доменного исключения другим;
* содержать логику конкретного Use Case.

---

### Целевой поток выполнения

```text
HTTP request
    ↓
Controller
    ↓
AddRequisitionCommentUseCase
    ↓
RequisitionGateway
    ↓
External system
```

Успешный сценарий:

```text
External system response
    ↓
Use Case проверяет ответ
    ↓
Use Case преобразует ответ в выходной DTO
    ↓
Controller возвращает ResponseEntity<ConcreteDto>
```

Ошибочный сценарий:

```text
External system response / technical exception
    ↓
Use Case определяет смысл ошибки
    ↓
Use Case выбрасывает DomainException
    ↓
DomainExceptionHandler формирует ApiError
```

---

### Целевой контроллер

После выполнения задачи контроллер должен выглядеть следующим образом:

```java
@PostMapping("/requisitions/comments")
public ResponseEntity<RequisitionGeneralCandidateDtoOut>
postRequisitionAddComment(
        @Valid
        @RequestBody RequisitionAddCommentRequest requestDto,
        @SapSystemParams SapSystemParamsDto systemParams
) {
    RequisitionGeneralCandidateDtoOut response =
            addRequisitionCommentUseCase.execute(
                    systemParams,
                    requestDto
            );

    return ResponseEntity.ok(response);
}
```

В контроллере отсутствуют:

* `if` по результатам интеграционного вызова;
* проверка ответа на `null`;
* разбор `responseCode`;
* создание исключений;
* ручное копирование DTO;
* интеграционные детали.

---

### Требуемые изменения

1. Создать отдельный Use Case для рефакторируемого вызова контроллера:

```java
AddRequisitionCommentUseCase
```

2. Перенести в Use Case:

* вызов соответствующего gateway или сервиса;
* проверку результата на `null`;
* проверку кода ответа;
* определение успешного и ошибочного сценария;
* выбрасывание наследников `DomainException`;
* преобразование результата в выходной DTO.

3. Перенести обработку низкоуровневых технических исключений в Use Case или специальный интеграционный сервис.

4. Исключить прямое использование в контроллере:

```java
HttpResponseException
```

5. Контроллер должен вызывать только соответствующий Use Case.

6. Не передавать `ResponseEntity` в Use Case.

7. Не использовать в Use Case классы Servlet API:

```java
HttpServletRequest
HttpServletResponse
ResponseEntity
```

8. HTTP-статус ошибки должен определяться через соответствующий `DomainException`.

9. Успешный результат Use Case должен быть типизированным DTO.

---

### Размещение классов

Вариант структуры:

```text
requisition
├── api
│   ├── RequisitionController
│   ├── RequisitionAddCommentRequest
│   └── RequisitionGeneralCandidateDtoOut
│
├── application
│   ├── usecase
│   │   ├── AddRequisitionCommentUseCase
│   │   └── AddRequisitionCommentUseCaseImpl
│   │
│   └── exception
│       ├── RequisitionNotFoundException
│       └── InvalidRequisitionStateException
│
├── domain
│   └── ...
│
└── infrastructure
    ├── RequisitionGateway
    └── EasupRequisitionAdapter
```

Если в проекте нет отдельного application-слоя, Use Case может размещаться в существующем сервисном слое, но ответственность должна оставаться такой же.

---

### Тестирование

Добавить unit-тесты Use Case для следующих сценариев:

1. Внешняя система вернула успешный ответ.
2. Внешняя система вернула `null`.
3. Внешняя система вернула ошибочный статус `4xx`.
4. Внешняя система вернула ошибочный статус `5xx`.
5. Внешняя система вернула пустой `responseCode`.
6. Внешняя система вернула некорректный `responseCode`.
7. Интеграционный адаптер завершился по таймауту.
8. Интеграционный адаптер выбросил неизвестное техническое исключение.
9. Успешный ответ корректно преобразован в выходной DTO.

Пример теста:

```java
@Test
void shouldThrowExternalSystemUnavailableExceptionWhenResponseIsNull() {
    when(requisitionGateway.addComment(
            systemParams,
            request
    )).thenReturn(null);

    assertThrows(
            ExternalSystemUnavailableException.class,
            () -> useCase.execute(systemParams, request)
    );
}
```

---

### Критерии приёмки

* Для вызова контроллера создан отдельный Use Case.
* Контроллер вызывает Use Case, а не содержит прикладную логику сценария.
* Проверка ответа внешней системы перенесена из контроллера в Use Case.
* Логика определения и выбрасывания `DomainException` находится в Use Case.
* Контроллер не проверяет результат на `null`.
* Контроллер не анализирует `responseCode`.
* Контроллер не создаёт `HttpResponseException`.
* Низкоуровневые технические исключения не передаются напрямую в контроллер.
* Use Case преобразовывает ожидаемые ошибки в соответствующие наследники `DomainException`.
* `DomainExceptionHandler` отвечает только за преобразование исключения в HTTP-ответ.
* Use Case не зависит от Servlet API и `ResponseEntity`.
* Успешный результат Use Case представлен конкретным DTO.
* Добавлены unit-тесты успешных и ошибочных сценариев Use Case.
* Существующий HTTP-контракт успешного ответа не изменён.
* Ошибочные ответы формируются в едином формате `ApiError`.
