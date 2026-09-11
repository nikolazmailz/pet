package ru.ntdev.srhr.ms.mock;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "server.port=0",
        "mock.wiremock.port=0",
        "mock.wiremock.verbose=false"
})
class WireMockStartupTest {

    @Autowired
    private WireMockServer wireMockServer;

    @Test
    void shouldStartWireMockAndLoadMappings() throws Exception {
        assertThat(wireMockServer.isRunning()).isTrue();
        assertThat(wireMockServer.getStubMappings()).isNotEmpty();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(wireMockServer.baseUrl() + "/functional-role/test-user/WEB"))
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"roleId\": \"admin\"", "\"roleId\": \"user\"");
    }
}
