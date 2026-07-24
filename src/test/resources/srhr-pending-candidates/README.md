# Кандидаты, ожидающие решения — реализация

Полная реализация фичи по 13 шагам постановки. Код раскладывается по четырём модулям монорепо; пакеты — `ru.ntdev.srhr.*`.

## Поток

```
фронт
  → srhr-ms-requisition-rest  POST /pending-candidates        (pernr из JWT)
  → Kafka srhr.requisition.to.easup  (req-reply через srhr-reqreply-starter)
  → srhr-ms-requisition  PendingCandidatesKafkaListener
      → refresh: HTTP → srhr-ms-master-data-integration POST /pending-candidates
                        → SAPPO POST /PendingCandidates
        транзакция: advisory lock(pernr) → delete → insert
      → query: count + страница id + fetch с events + фасет + обогащение заявкой
  → Kafka srhr.requisition.from.easup (+correlation id)
  → rest → фронт (error → 502, таймаут → 504)
```

## Карта файлов по шагам

| Шаг | Что | Где |
|---|---|---|
| 1 | Миграции Liquibase | `srhr-ms-requisition/src/main/resources/db/changelog/pending-candidates/2026-07-pending-candidates.xml` |
| 2 | Сущности | `requisition/.../entity/PendingCandidate.java`, `PendingCandidateEvent.java` |
| 3 | DTO + тесты сериализации | `srhr-common/.../dto/pendingcandidates/**` |
| 4 | Клиент SAPPO | `masterdataintegration/.../client/EstaffClient.java` |
| 5 | Прокси-эндпоинт + тесты | `masterdataintegration/.../web/**`, тесты в `src/test` |
| 6 | Клиент интеграции | `requisition/.../client/MasterDataIntegrationClient.java` |
| 7 | Репозиторий + кастомные запросы | `requisition/.../repository/**` |
| 8 | Маппер + refresh-сервис | `requisition/.../mapper/PendingCandidateMapper.java`, `service/PendingCandidatesRefreshService.java` |
| 9 | Query-сервис + порт заявок | `service/PendingCandidatesQueryService.java`, `RequisitionLookupPort.java` |
| 10 | Kafka-listener | `requisition/.../kafka/PendingCandidatesKafkaListener.java` |
| 11 | REST-фасад | `srhr-ms-requisition-rest/.../pendingcandidates/**` |
| 12 | Тесты | `requisition/src/test/.../PendingCandidateMapperTest`, `PendingCandidatesRefreshServiceIT`, `PendingCandidatesQueryServiceIT` |
| 13 | Конфигурация | `config/**` + `application-pending-candidates-example.yml` в каждом сервисе |

## Точки интеграции с существующим кодом (ДОПУЩЕНИЯ — проверить при вливании)

1. **`integrationRestTemplate`** — общий singleton `RestTemplate` из mod-commons (`@AutoConfiguration`, пул Apache HttpClient 5). Оба клиента (`EstaffClient`, `MasterDataIntegrationClient`) инжектят его по имени бина. Таймауты — в его конфигурации, здесь не дублируются.
2. **`srhr-reqreply-starter`** — предполагаемый API: `KafkaRedisRequestReplyTemplate<Req, Resp>#sendAndReceive(topic, request, responseType, timeout)` и `ReplyTimeoutException`. Если фактическая сигнатура иная — правка только в `PendingCandidatesRestController`.
3. **`RequisitionLookupPort`** — интерфейс без реализации, намеренно. Реализация — адаптер над существующим `RequisitionRepository` (`findByVacancyIdIn(Collection<String>)`, ОДИН batch-запрос) + маппинг `Requisition → RequisitionShortDto`. Написать её можно только глядя на реальную entity `Requisition` — контракт и ожидания зафиксированы в javadoc порта. В тестах порт замокан.
4. **JWT-claim табельного номера** — `CurrentUserPernrResolver` ожидает claim `pernr` в `Jwt` principal (общий `JwtTokenFilter` из srhr-common). Если claim называется иначе — правка в одной точке.
5. **Kafka-инфраструктура** — listener ссылается на `pendingCandidatesListenerContainerFactory` и типизированный `KafkaTemplate<String, PendingCandidatesResponse>`; их объявление — по образцу существующих фабрик сервиса (JSON serde, error handler). `spring.kafka.consumer.group-id` — из общей конфигурации сервиса.
6. **Master changelog** — добавить `<include file="db/changelog/pending-candidates/2026-07-pending-candidates.xml"/>`.

## Ключевые решения в коде

- **Advisory lock до delete, HTTP до транзакции.** `pg_advisory_xact_lock(hashtext(pernr))` сериализует конкурентные refresh одного pernr; запрос в Е-стафф выполняется до открытия транзакции — соединение с БД и лок не держатся на время сетевого вызова, при ошибке интеграции локальные данные не тронуты. Покрыто конкурентным тестом.
- **Страница за два запроса**: сначала id страницы (native SQL, `GROUP BY` + `ORDER BY min(status_date) NULLS LAST, full_name, id`, `LIMIT/OFFSET`), затем `join fetch` events по `id IN (...)` с восстановлением порядка в сервисе. Пагинация поверх `join fetch` в один запрос — известная ловушка Hibernate (in-memory paging).
- **Экранирование LIKE**: `%`/`_`/`\` в поиске — литералы (`escape '\'`), покрыто тестом.
- **Фасет** не учитывает фильтр `eventCodeList` (снятие фильтра в UI), учитывает поиск — зафиксировано тестами `Facet`.
- **Bulk delete + FK ON DELETE CASCADE**: JPQL-delete обходит JPA-каскады, события чистит БД.
- **Refresh-IT без `@Transactional`** на классе — иначе тестовый rollback скрыл бы реальные транзакционные границы; изоляция через уникальные pernr.

## Известные ограничения (задокументированы в javadoc)

- Пагинация неконсистентна между запросами страниц: каждый запрос перезагружает снимок из Е-стафф (решение №10 постановки).
- `count` считается отдельным запросом от страницы — теоретический разрыв в рамках одного запроса отсутствует (оба читают в одной readonly-транзакции после refresh).
