package ru.ntdev.srhr.mdi;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import ru.ntdev.srhr.pending.contracts.PendingCandidatesSnapshotResponse;
import ru.ntdev.srhr.pending.contracts.PernrRequest;
import ru.ntdev.srhr.pending.contracts.PernrRequestEnvelope;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PendingCandidatesIntegrationTest {
    static final WireMockServer WIRE_MOCK = new WireMockServer(0);

    @LocalServerPort int port;
    @Autowired TestRestTemplate restTemplate;

    @BeforeAll static void start() { WIRE_MOCK.start(); }
    @AfterAll static void stop() { WIRE_MOCK.stop(); }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("integration.sappo.base-url", WIRE_MOCK::baseUrl);
    }

    @Test
    void mapsSappoResponse() {
        WIRE_MOCK.stubFor(post(urlEqualTo("/PendingCandidates"))
                .willReturn(okJson("""
                    {"data":{"candidates":[{"candidateId":1234567890123456789,"vacancyId":"42","fullName":"Иванов Иван","events":[{"eventCode":"rr_resume_review","statusDate":1701565600000,"days":3,"expirationZone":2}]}]}}
                    """)));

        PendingCandidatesSnapshotResponse response = restTemplate.postForObject(
                "http://localhost:" + port + "/pending-candidates",
                new PernrRequestEnvelope(new PernrRequest("12345678")),
                PendingCandidatesSnapshotResponse.class
        );

        assertThat(response).isNotNull();
        assertThat(response.data().candidates()).hasSize(1);
        assertThat(response.data().candidates().get(0).candidateId()).isEqualTo("1234567890123456789");
    }
}
