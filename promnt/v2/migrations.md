# Миграции базы данных (Liquibase)

Использовать Liquibase.

Правила:
- В db.changelog-master.yaml указываются ТОЛЬКО include-файлы

Пример:
databaseChangeLog:
- include:
  file: changes/0001-create-message-queue.sql
  relativeToChangelogFile: true

- Сами миграции пишутся:
    - только в .sql файлах
