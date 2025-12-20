# Репозитории: только DatabaseClient + JSON из БД

Правила репозитория:
- Работа с БД разрешена только через DatabaseClient
- Не использовать Spring Data репозитории (R2dbcRepository и т.п.)
- SQL пишется вручную через client.sql("""...""".trimIndent())

Правила получения данных:
- Все SELECT-запросы должны возвращать ровно один столбец:
    - jsonb_build_object(...) as data
- Маппинг результата:
    - objectMapper.readValue<YourEntity>((it["data"] as Json).asString())

Паттерны:
- findById / single:
    - fetch().one() ... awaitSingleOrNull()
- findAll / list:
    - fetch().all() ... collectList().awaitSingleOrNull() ?: emptyList()
- insert/update returning:
    - использовать returning и затем select нужных полей
    - при необходимости — CTE with updated/inserted

Нельзя:
- возвращать из SELECT отдельные колонки и маппить вручную
- использовать RowMapper / custom converters вместо jsonb_build_object (если не указано иначе)
