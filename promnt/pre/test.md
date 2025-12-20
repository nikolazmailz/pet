Тесты е2е с одним родительским классом BaseIntegrationTest.kt
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class AbstractIntegrationTest(body: ShouldSpec.() -> Unit = {}) : ShouldSpec(body) {
// code
}
Все e2e тесты обязаны наследоваться от BaseIntegrationTest.

Kotest
testcontainers
wiremock

Правила:
e2e тесты поднимают реальный Spring context
тестируют фичу целиком: controller → application → infra → БД

