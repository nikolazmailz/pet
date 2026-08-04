package ru.ntdev.srhr.masterdataintegration.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import ru.ntdev.srhr.masterdataintegration.config.EstaffProperties;
import ru.ntdev.srhr.masterdataintegration.exception.EstaffIntegrationException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = EstaffClientTest.TestConfig.class,
        properties = "estaff.base-url=${wiremock.server.baseUrl}")
@EnableWireMock
@DisplayName("EstaffClient")
class EstaffClientTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        RestTemplate integrationRestTemplate() {
            return new RestTemplate();
        }

        @Bean
        EstaffProperties estaffProperties(
                @org.springframework.beans.factory.annotation.Value("${estaff.base-url}") String baseUrl) {
            return new EstaffProperties(baseUrl);
        }

        @Bean
        EstaffClient estaffClient(RestTemplate integrationRestTemplate, EstaffProperties props) {
            return new EstaffClient(integrationRestTemplate, props);
        }
    }

    @Autowired
    private EstaffClient client;

    @InjectWireMock
    private WireMockServer wireMock;

    @BeforeEach
    void resetStubs() {
        wireMock.resetAll();
    }

    @Test
    @DisplayName("отправляет pernr в обёртке request и парсит числовые id в String")
    void sendsWrappedRequestAndParsesResponse() {
        wireMock.stubFor(post(urlEqualTo("/PendingCandidates"))
                .withRequestBody(equalToJson("""
                        { "request": { "pernr": "12345678" } }
                        """))
                .willReturn(okJson("""
                        { "data": { "candidates": [{
                            "candidateId": 1234567890123456789,
                            "vacancyId": 987, "fullName": "Иванов",
                            "events": [] }] } }
                        """)));

        var response = client.fetchPendingCandidates("12345678");

        assertThat(response.data().candidates())
                .singleElement()
                .satisfies(c -> assertThat(c.candidateId()).isEqualTo("1234567890123456789"));
    }

    @Test
    @DisplayName("5xx от SAPPO -> EstaffIntegrationException")
    void serverErrorWrapped() {
        wireMock.stubFor(post(urlEqualTo("/PendingCandidates"))
                .willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> client.fetchPendingCandidates("12345678"))
                .isInstanceOf(EstaffIntegrationException.class);
    }
}
