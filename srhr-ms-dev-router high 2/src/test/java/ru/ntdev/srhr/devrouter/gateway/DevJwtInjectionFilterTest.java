package ru.ntdev.srhr.devrouter.gateway;

import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.ntdev.srhr.devrouter.jwt.DevJwtProperties;
import ru.ntdev.srhr.devrouter.jwt.DevJwtService;
import ru.ntdev.srhr.devrouter.session.DevSessionProperties;
import ru.ntdev.srhr.devrouter.session.DevUserSelectionResolver;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class DevJwtInjectionFilterTest {

    @Test
    void replacesIncomingAuthorizationAndInjectsSelectedDevUser() throws Exception {
        DevJwtProperties jwtProperties = properties();
        DevSessionProperties sessionProperties = new DevSessionProperties();
        DevJwtInjectionFilter filter = new DevJwtInjectionFilter(
                new DevJwtService(jwtProperties),
                new DevUserSelectionResolver(jwtProperties, sessionProperties)
        );
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/ui-api-web/cross/srhr/paystub/v1/paystub/period")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer corporate-token-must-not-pass")
                        .header(DevJwtInjectionFilter.DEV_USER_HEADER, "developer")
        );
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();
        GatewayFilterChain chain = current -> {
            captured.set(current);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        String authorization = captured.get().getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        assertThat(authorization).startsWith("Bearer ");
        assertThat(authorization).doesNotContain("corporate-token-must-not-pass");
        SignedJWT jwt = SignedJWT.parse(authorization.substring("Bearer ".length()));
        assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo("developer@VTB.RU");
        assertThat(captured.get().getRequest().getHeaders().getFirst("X-Dev-Router")).isEqualTo("true");
        assertThat(captured.get().getRequest().getHeaders().getFirst("X-Request-ID")).isNotBlank();
    }

    private static DevJwtProperties properties() {
        DevJwtProperties properties = new DevJwtProperties();
        properties.setKeyId("test-key");
        properties.setTtl(Duration.ofMinutes(10));
        properties.setDefaultUser("developer");

        DevJwtProperties.UserProfile developer = new DevJwtProperties.UserProfile();
        developer.setSub("developer@VTB.RU");
        developer.setSessionId("test-session");
        developer.setChannel("WEB");
        developer.setRealm("SRHR");
        properties.setUsers(Map.of("developer", developer));
        return properties;
    }
}
