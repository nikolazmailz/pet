# BaseIntegrationTest

Все e2e тесты ОБЯЗАНЫ наследоваться от BaseIntegrationTest.

Пример:

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class AbstractIntegrationTest(
body: ShouldSpec.() -> Unit = {}
) : ShouldSpec(body) {
// общая конфигурация, testcontainers, wiremock
}
