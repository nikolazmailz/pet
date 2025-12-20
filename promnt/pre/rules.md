Вначале всегда предоставь план, того, что собираешься сделать, чтоб получить подтверждение на то или иное или на все.

Не нужно генерировать кода больше чем в заявленно. Можно комментировать, что бы ты ещё добавил. Всегда придерживаться best practice.

Приложение должно быть организовано по Clean Architecture (Robert C. Martin):
Пакеты верхнего уровня:
com/example/domain
com/example/application
com/example/controllers
com/example/infra
com/example/config

Корутины:
API везде через suspend функции (где уместно).
Не использовать блокирующие вызовы (block(), Thread.sleep, синхронный JDBC), кроме специальных случаев (и тогда явно объяснять почему).

Контроллеры:
Всегда добавлять openApi (валидация делается на входящих Dto). Во входящих dto использовать @Schema и добавлять описание на русском.
Использовать coroutine-friendly endpoints: suspend fun в @RestController.
Не возвращать Mono/Flux, если можно suspend + обычные типы / Flow (если нужна стриминг-модель).
Валидация DTO: если используешь Bean Validation — объясни где и как (и как работает в WebFlux).

Ошибки:
Единая обработка ошибок через @RestControllerAdvice (или functional error handler, если выбрано).
Возвращать предсказуемый формат ошибок (problem+json или свой ErrorResponse) — выбрать один подход и придерживаться.

Миграции: Liquibase
Миграции пишутся так: в db.changelog-master.yaml пишутся только подключаемые файлы
по примеру:
databaseChangeLog:
- include:
  file: changes/0001-create-message-queue.sql
  relativeToChangelogFile: true

сами миграции пишутся в файлах .sql


Если тебе не хватает входных данных, задай уточняющие вопросы перед тем, как писать код.
Если данных достаточно — не задавай вопросов и сразу пиши решение.


