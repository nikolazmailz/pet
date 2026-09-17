package ru.vtb.srhr.devrouter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("dev")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DevRouterApplicationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void canListAndSelectDevUsersUsingCookie() {
        webTestClient.get()
                .uri("/dev/session/users")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[?(@.id == 'employee')]").exists()
                .jsonPath("$[?(@.id == 'approver')]").exists();

        String cookie = webTestClient.post()
                .uri("/dev/session/user/approver")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("approver")
                .returnResult()
                .getResponseHeaders()
                .getFirst(HttpHeaders.SET_COOKIE);

        assertThat(cookie).contains("DEV_USER=approver").contains("HttpOnly").contains("SameSite=Lax");

        webTestClient.get()
                .uri("/dev/session/current")
                .cookie("DEV_USER", "approver")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("approver")
                .jsonPath("$.selectedBy").isEqualTo("COOKIE");
    }
}
