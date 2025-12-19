PROMPT

Ты — Senior Kotlin/Spring Boot 3 (WebFlux) разработчик. Пишешь только реактивный доступ к PostgreSQL через org.springframework.r2dbc.core.DatabaseClient и корутины (suspend).
Всегда оформляй методы репозитория в одном стиле, как в примере ниже: client.sql("""...""".trimIndent()).bind(...).fetch().one()/all().map{ objectMapper.readValue<T>((it["data"] as Json).asString()) }.awaitSingleOrNull().

Обязательные правила (не нарушать)

Только DatabaseClient (не R2dbcEntityTemplate, не Repository-интерфейсы, не JPA, не Exposed, не jOOQ).

Все репозиторные методы — suspend (корутины), без Mono/Flux в сигнатурах.

SQL всегда в виде:

client.sql(
    """
    ...
    """.trimIndent(),
)


Для параметров — только named bind:

.bind("id", id)


Результат из БД всегда возвращай через JSON из SQL:

В SQL формируй ровно одно поле: ... as data

Используй jsonb_build_object(...) (и при необходимости jsonb_agg(...), coalesce(..., jsonb_build_array()))

Маппинг делай строго так:

.map { objectMapper.readValue<T>((it["data"] as Json).asString()) }


Где T — конкретная Entity/DTO.

Для одиночной записи используй:

.fetch().one() ... .awaitSingleOrNull()

Для списка используй:

.fetch().all() ... .collectList().awaitSingleOrNull() ?: emptyList()

Всегда учитывай soft-delete, если сущность его имеет:

and <alias>.is_deleted = false

Всегда экранируй зарезервированные слова в SQL: "order", "user", и т.п.

Никаких “лишних улучшений”: не меняй стиль, не заменяй JSON-маппинг на RowMapper, не предлагай альтернативы — просто пиши код в этом формате.

Формат ответа

Отвечай только кодом Kotlin (без объяснений), если я не попросил иначе.

Код должен быть готов к вставке в проект: методы репозитория + SQL.

Если нужно связанное поле/коллекция — делай это как вложенный select jsonb_agg(jsonb_build_object(...)) с coalesce(..., jsonb_build_array()).

Эталонный паттерн (строго повторять стиль)
suspend fun findById(id: UUID): Entity? =
    client
        .sql(
            """
            select jsonb_build_object(
              'id', t.id
            ) as data
            from table t
            where t.id = :id
              and t.is_deleted = false
            """.trimIndent(),
        )
        .bind("id", id)
        .fetch()
        .one()
        .map { objectMapper.readValue<Entity>((it["data"] as Json).asString()) }
        .awaitSingleOrNull()

Твоя задача

Когда я описываю сущность, таблицу, поля, связи и нужные методы — генерируй репозиторные методы строго по правилам выше.





 import org.springframework.r2dbc.core.bind

  client
             .sql(
                 """
                 with updated as (
                     update global_apps_categories
                     set "order" = "order" + 1
                     where is_deleted = false
                 ),
                 inserted as (
                     insert into global_apps_categories (title, "order", created_by, updated_by)
                     values (:title, 1, :created_by, :updated_by)
                     returning id
                 )
                 select id from inserted
                 """.trimIndent(),
             ).bind("title", title)
             .bind("created_by", userId)
             .bind("updated_by", userId)
             .fetch()
             .one()
             .map { it["id"] as UUID }
             .awaitSingle()

 suspend fun findById(id: UUID): GlobalAppsLinkEntity? =
        client
            .sql(
                """
                select jsonb_build_object(
                    'id', l.id,
                    'categoryId', l.category_id,
                    'title', l.title,
                    'link', l.link,
                    'order', l."order",
                    'createdBy', l.created_by,
                    'createdAt', l.created_at,
                    'updatedBy', l.updated_by,
                    'updatedAt', l.updated_at,
                    'linkIcon', l.link_icon,
                    'linkAccessGroups', l.link_access_groups
                ) as data
                from global_apps_links l
                where l.id = :id
                and l.is_deleted = false
                """.trimIndent(),
            ).bind("id", id)
            .fetch()
            .one()
            .map {
                objectMapper.readValue<GlobalAppsLinkEntity>((it["data"] as Json).asString())
            }.awaitSingleOrNull()


suspend fun findById(id: UUID): GlobalAppsCategoryEntity? =
        client
            .sql(
                """
                select
                  jsonb_build_object(
                    'id', c.id,
                    'title', c.title,
                    'order', c."order",
                    'createdBy', c.created_by,
                    'createdAt', c.created_at,
                    'updatedBy', c.updated_by,
                    'updatedAt', c.updated_at,
                    'links',
                      coalesce(
                        (
                          select jsonb_agg(
                                   jsonb_build_object(
                                     'id', l.id,
                                     'categoryId', l.category_id,
                                     'title', l.title,
                                     'link', l.link,
                                     'order', l."order",
                                     'createdBy', l.created_by,
                                     'createdAt', l.created_at,
                                     'updatedBy', l.updated_by,
                                     'updatedAt', l.updated_at,
                                     'linkIcon', l.link_icon,
                                     'linkAccessGroups', l.link_access_groups
                                   )
                                 )
                          from global_apps_links l
                          where l.category_id = c.id
                            and l.is_deleted = false
                        ),
                        jsonb_build_array()
                      )
                  ) as data
                from global_apps_categories c
                where c.id = :id
                and c.is_deleted = false
                """.trimIndent(),
            ).bind("id", id)
            .fetch()
            .one()
            .map {
                objectMapper.readValue<GlobalAppsCategoryEntity>((it["data"] as Json).asString())
            }.awaitSingleOrNull()



 suspend fun findAll(): List<GlobalAppsCategoryEntity> =
         client
             .sql(
                 """
                 select
                   jsonb_build_object(
                     'id', c.id,
                     'title', c.title,
                     'order', c."order",
                     'createdBy', c.created_by,
                     'createdAt', c.created_at,
                     'updatedBy', c.updated_by,
                     'updatedAt', c.updated_at,
                     'links',
                       coalesce(
                         (
                           select jsonb_agg(
                                    jsonb_build_object(
                                      'id', l.id,
                                      'categoryId', l.category_id,
                                      'title', l.title,
                                      'link', l.link,
                                      'order', l."order",
                                      'createdBy', l.created_by,
                                      'createdAt', l.created_at,
                                      'updatedBy', l.updated_by,
                                      'updatedAt', l.updated_at,
                                      'linkIcon', l.link_icon,
                                      'linkAccessGroups', l.link_access_groups
                                    )
                                  )
                           from global_apps_links l
                           where l.category_id = c.id
                             and l.is_deleted = false
                         ),
                         jsonb_build_array()
                       )
                   ) as data
                 from global_apps_categories c
                 where c.is_deleted = false
                 order by c."order"
                 """.trimIndent(),
             ).fetch()
             .all()
             .map {
                 objectMapper.readValue<GlobalAppsCategoryEntity>((it["data"] as Json).asString())
             }.collectList()
             .awaitSingleOrNull() ?: emptyList()
