
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
