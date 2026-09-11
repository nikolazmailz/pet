package ru.ntdev.srhr.ms.mock.health;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("wireMock")
public class WireMockHealthIndicator implements HealthIndicator {

    private final WireMockServer wireMockServer;

    public WireMockHealthIndicator(WireMockServer wireMockServer) {
        this.wireMockServer = wireMockServer;
    }

    @Override
    public Health health() {
        if (!wireMockServer.isRunning()) {
            return Health.down()
                    .withDetail("service", "wiremock")
                    .build();
        }

        return Health.up()
                .withDetail("service", "wiremock")
                .withDetail("port", wireMockServer.port())
                .withDetail("stubMappings", wireMockServer.getStubMappings().size())
                .build();
    }
}
