# Промпт: реализация фичи «Кандидаты, ожидающие решения»

## Роль

Ты — senior Java/Spring Boot разработчик. Реализуй фичу строго по шагам ниже. Каждый шаг — отдельный, самодостаточный кусок работы (эквивалент одного небольшого PR): выполняй их по порядку, в конце каждого шага показывай полный код созданных/изменённых файлов и краткое пояснение (2–3 предложения). Не объединяй шаги. Если чего-то не хватает в постановке — не выдумывай молча: явно напиши «ДОПУЩЕНИЕ: ...» и продолжай.

## Стек и конвенции (обязательны)

- Java 17, Spring Boot 3.2, Maven.
- PostgreSQL + Liquibase (миграции в формате XML changelog), JPA/Hibernate.
- Apache Kafka (spring-kafka), Redis (Lettuce).
- Lombok: `@RequiredArgsConstructor`, конструкторная инъекция. Полевые `@Value`/`@Autowired` запрещены.
- MapStruct для маппинга DTO ↔ entity.
- Конфигурационные классы: `@Configuration(proxyBeanMethods = false)`.
- Тесты: JUnit 5 + Mockito + AssertJ, `@Nested` + `@DisplayName` для структуры; интеграционные — Testcontainers (PostgreSQL). Помни: `@Transactional` в тестах откатывается — для проверки реальных транзакционных границ используй `TransactionTemplate`.
- Пакеты: `ru.ntdev.srhr.<service>...`.

## Архитектурный контекст

Три микросервиса:

1. **srhr-ms-requisition-rest** — REST-фасад для фронта. Принимает `POST /pending-candidates`, отправляет запрос в Kafka-топик `srhr.requisition.to.easup`, ждёт ответ из `srhr.requisition.from.easup` через существующий механизм request-reply (Kafka + Redis, correlation id). Считай, что в проекте уже есть библиотека `srhr-reqreply-starter` с классом `KafkaRedisRequestReplyTemplate<Req, Resp>` (метод `Resp sendAndReceive(String topic, Req request, Class<Resp> respType, Duration timeout)`), — используй её, не реализуй транспорт заново.
2. **srhr-ms-requisition** — бизнес-логика. Слушает топик, ходит в Е-стафф через сервис интеграции, персистит, фильтрует, пагинирует, обогащает заявкой, отвечает в reply-топик.
3. **srhr-ms-master-data-integration** — тонкий HTTP-прокси к SAPPO (Е-стафф): `POST /pending-candidates` → SAPPO `POST /PendingCandidates`.

Поток: фронт → rest (Kafka req-reply) → requisition → (HTTP) → master-data-integration → (HTTP) → SAPPO.

## Зафиксированные проектные решения (не обсуждаются, применяй как данность)

1. Колонка называется `approver_pernr` (в исходной постановке была опечатка `approve_pernr`).
2. Все внешние идентификаторы (`candidate_id`, `vacancy_id` из Е-стафф) — **VARCHAR в БД и String в Java/JSON**: значения 19-значные, в JSON фронту отдаются строками во избежание потери точности в JS. FK на внутреннюю таблицу `vacancy` НЕ делаем — `vacancy_id` хранит внешний идентификатор без ограничения ссылочной целостности.
3. `status_date` — `TIMESTAMP WITH TIME ZONE`, из Е-стафф приходит epoch millis (UTC), маппить в `Instant`.
4. Сортировка списка кандидатов: по возрастанию ближайшего дедлайна — `MIN(events.status_date)` по кандидату, затем `full_name ASC`, затем `id ASC` (стабильность пагинации).
5. Фасет `eventCode` в ответе: учитывает поиск по ФИО, но **НЕ учитывает** фильтр `eventCodeList` (иначе пользователь не сможет снять фильтр в UI). Отрази это допущение комментарием в коде.
6. Замена данных (delete + insert) по `approver_pernr` выполняется в одной транзакции под advisory-lock PostgreSQL: `pg_advisory_xact_lock(hashtext(:pernr))` — защита от гонки параллельных запросов одного пользователя.
7. Поиск по ФИО: `ILIKE '%' || :search || '%'`, спецсимволы `%`/`_` экранировать.
8. Фильтр `eventCodeList` — условие ИЛИ: кандидат попадает в выборку, если у него есть хотя бы один этап из списка (EXISTS-подзапрос).
9. Обогащение заявкой: по `vacancy_id` кандидата ищется заявка (считай, что в srhr-ms-requisition есть `RequisitionRepository.findByVacancyId(String)` и entity `Requisition` с полями guid, number, positionType, staffPosition(id,name), structUnitName, structUnitId, structUnitPathList). Если заявка не найдена — `requisition: null`, кандидата НЕ выкидывать.
10. Пагинация: `page` начинается с 1; `pageSize` максимум 100; offset-пагинация. Неконсистентность между страницами (данные перезагружаются каждым запросом) — принятое ограничение, задокументируй в javadoc сервиса.
11. Ошибка Е-стафф (таймаут, 5xx): бизнес-исключение → в reply уходит ответ с полем `error` (код + сообщение), rest транслирует как HTTP 502 с телом `{ "error": { "code": "ESTAFF_UNAVAILABLE", "message": ... } }`. Локальные данные при ошибке НЕ трогать.

## Шаги реализации

### Шаг 1. Liquibase-миграции (srhr-ms-requisition)

Создай changelog с двумя таблицами:

- `pending_candidates`: `id BIGSERIAL PK`, `vacancy_id VARCHAR(32) NOT NULL`, `candidate_id VARCHAR(32) NOT NULL`, `full_name VARCHAR(255) NOT NULL`, `approver_pernr VARCHAR(16) NOT NULL`. Индекс по `approver_pernr`.
- `pending_candidates_events`: `id BIGSERIAL PK`, `pending_candidate_id BIGINT NOT NULL` FK → `pending_candidates(id)` ON DELETE CASCADE, `event_code VARCHAR(64) NOT NULL`, `status_date TIMESTAMPTZ NOT NULL`, `days INTEGER`, `expiration_zone INTEGER`. Индексы по `pending_candidate_id` и `event_code`.

Результат: XML changelog + include в master changelog.

### Шаг 2. JPA-сущности (srhr-ms-requisition)

`PendingCandidate` (OneToMany на события, `cascade = ALL`, `orphanRemoval = true`) и `PendingCandidateEvent` (ManyToOne LAZY). Lombok `@Getter/@Setter`, без `@Data` на entity. `equals/hashCode` по id с учётом Hibernate-прокси (или не переопределять — обоснуй выбор одной строкой).

### Шаг 3. DTO-контракты (общий модуль или дублирование по сервисам — выбери и обоснуй)

- Интеграционные DTO (Е-стафф): `EstaffPendingCandidatesRequest { pernr }`, `EstaffPendingCandidatesResponse { data.candidates[]: { candidateId, vacancyId, fullName, events[]: { eventCode, statusDate(epoch millis), days, expirationZone } } }`.
- REST/Kafka DTO: запрос `PendingCandidatesRequest { page, pageSize, filter { search, eventCodeList }, pernr }` (pernr rest-сервис берёт из JWT текущего пользователя и добавляет сам — с фронта не принимается); ответ `PendingCandidatesResponse { data { page, pageSize, count, eventCode[]: { codeValue }, candidates[]: { candidateId, vacancyId, fullName, requisition {...}, events[]: { code, days, expirationZone } } }, error }`.

Валидация: `page >= 1`, `1 <= pageSize <= 100`, `search` — максимум 255 символов.

### Шаг 4. Клиент SAPPO в srhr-ms-master-data-integration

`EstaffClient` на общем `integrationRestTemplate` (считай, что бин уже есть в контексте): `POST {estaff.base-url}/PendingCandidates`. Таймауты из конфигурации. Обработка не-2xx → выброс `EstaffIntegrationException`.

### Шаг 5. Эндпоинт `POST /pending-candidates` в srhr-ms-master-data-integration

Контроллер → клиент из шага 4 → ответ как есть (passthrough, без трансформаций). Обработчик `EstaffIntegrationException` → HTTP 502 с телом ошибки. Тест контроллера через `@WebMvcTest` + мок клиента; тест клиента через WireMock.

### Шаг 6. HTTP-клиент интеграции в srhr-ms-requisition

`MasterDataIntegrationClient`: `POST {master-data-integration.base-url}/pending-candidates`. Тот же паттерн, что в шаге 4. Свой exception `PendingCandidatesFetchException`.

### Шаг 7. Репозиторий и запросы (srhr-ms-requisition)

`PendingCandidateRepository` (Spring Data JPA) + кастомная часть на JPQL/нативном SQL. Три запроса с одинаковым WHERE-ядром (`approver_pernr = :pernr` + поиск + EXISTS по кодам):

1. Страница id кандидатов с сортировкой из решения №4 (сначала выбираем id страницы, затем fetch кандидатов с events по `id IN (...)` — избегаем pagination + join fetch проблемы).
2. `count(*)`.
3. Фасет: `SELECT DISTINCT e.event_code` — БЕЗ фильтра по кодам (решение №5).

Плюс `deleteByApproverPernr(String)`.

### Шаг 8. Сервис замены данных (srhr-ms-requisition)

`PendingCandidatesRefreshService.refresh(String pernr)`: одна транзакция → advisory lock (решение №6) → delete по pernr → маппинг ответа Е-стафф в entity (MapStruct, epoch millis → Instant) → saveAll. При исключении интеграции — откат, проброс наверх.

### Шаг 9. Сервис выборки (srhr-ms-requisition)

`PendingCandidatesQueryService.find(PendingCandidatesRequest)`: вызывает refresh → выполняет три запроса из шага 7 → сортирует events каждого кандидата по `status_date` asc (в памяти или `@OrderBy`) → обогащает заявкой через `RequisitionRepository` batch-запросом по всем `vacancy_id` страницы (один запрос, не N+1) → собирает ответ. Javadoc с ограничением из решения №10.

### Шаг 10. Kafka-обработчик (srhr-ms-requisition)

Listener на `srhr.requisition.to.easup`: десериализация запроса → `PendingCandidatesQueryService` → ответ в `srhr.requisition.from.easup` с correlation id из заголовков. Ошибки → ответ с заполненным `error` (решение №11), исключение не глотать молча — логировать.

### Шаг 11. REST-фасад (srhr-ms-requisition-rest)

Контроллер `POST /pending-candidates`: pernr из SecurityContext (JWT), валидация тела, `KafkaRedisRequestReplyTemplate.sendAndReceive(...)` с таймаутом из конфигурации → ответ фронту. `error` в ответе → HTTP 502. Таймаут req-reply → HTTP 504.

### Шаг 12. Тесты бизнес-логики

- `PendingCandidatesRefreshServiceTest`: интеграционный на Testcontainers — конкурентная проверка (два потока, один pernr, advisory lock: нет дублей и потерянных данных), корректный маппинг millis → Instant.
- `PendingCandidatesQueryServiceTest`: интеграционный — фильтр ИЛИ по кодам, поиск с экранированием `%`, сортировка по ближайшему дедлайну, фасет без учёта фильтра кодов, count с учётом фильтров, requisition = null при отсутствии заявки, отсутствие N+1 (проверка числа запросов не обязательна — достаточно batch-структуры кода).
- Юнит-тесты маппера.

### Шаг 13. Конфигурация и финализация

Все `@ConfigurationProperties` (base-url'ы, таймауты, kafka-топики) в одном месте на сервис, `application.yml` с примерами значений, README-фрагмент: схема потока, список допущений, известные ограничения (неконсистентность пагинации, семантика фасета).

## Формат ответа

По каждому шагу: заголовок шага → полные файлы кода → 2–3 предложения пояснений → список допущений (если были). Не сокращай код многоточиями.
