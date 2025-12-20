# Тестирование

Использовать:
- Kotest
- Testcontainers
- WireMock

Тип тестов:
- Только e2e / интеграционные тесты

Обязательные правила:
- Все e2e тесты поднимают реальный Spring context
- Тестируется фича целиком:
  controller → application → infra → БД
