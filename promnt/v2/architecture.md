# Архитектура

Приложение должно быть организовано по Clean Architecture (Robert C. Martin).

Пакеты верхнего уровня:
- com/example/domain
- com/example/application
- com/example/controllers
- com/example/infra
- com/example/config

Требования:
- Чёткое разделение слоёв
- Отсутствие утечек фреймворка в domain
- Зависимости направлены внутрь
