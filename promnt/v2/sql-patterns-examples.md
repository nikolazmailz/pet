# Эталонные паттерны SQL + DatabaseClient

## Insert/Update с CTE и returning id
Используй стиль:
- with updated as (...)
- inserted as (... returning id)
- select id from inserted
- bind(...) параметров
- fetch().one()
- map { it["id"] as UUID }
- awaitSingle()

## findById: возвращаем jsonb_build_object(...) as data
Всегда:
- select jsonb_build_object(...) as data
- where ... and is_deleted = false
- bind("id", id)
- fetch().one()
- map { objectMapper.readValue<Entity>((it["data"] as Json).asString()) }
- awaitSingleOrNull()

## findById с вложенной коллекцией
Всегда:
- links: coalesce((select jsonb_agg(jsonb_build_object(...)) ...), jsonb_build_array())
- весь объект собирается в jsonb_build_object(...) as data

## findAll
Всегда:
- fetch().all()
- map { objectMapper.readValue<Entity>((it["data"] as Json).asString()) }
- collectList()
- awaitSingleOrNull() ?: emptyList()

## Примечания
- Название алиаса всегда: data
- Используй bind(...) из org.springframework.r2dbc.core.bind
