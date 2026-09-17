package ru.vtb.srhr.devrouter.impersonation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.vtb.srhr.devrouter.config.DevRouterProperties;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class DevUserHeadersFilterTest {

    private static final Instant NOW = Instant.parse("2026-09-17T12:00:00Z");

    @Test
    void replacesSpoofedHeadersWithConfiguredProfile() {
        DevRouterProperties properties = DevUserResolverTest.properties();
        var resolver = new DevUserResolver(properties);
        var filter = new DevUserHeadersFilter(
                properties,
                resolver,
                new ObjectMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        var request = MockServerHttpRequest.get("/ui-api-web/cross/srhr/paystub/v1/documents")
                .header("X-Dev-User", "approver")
                .header("adLogin", "attacker")
                .header("roles", "SUPER_ADMIN")
                .header("sessionId", "forged-session")
                .build();
        var exchange = MockServerWebExchange.from(request);
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        GatewayFilterChain chain = filteredExchange -> {
            forwarded.set(filteredExchange);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        var headers = forwarded.get().getRequest().getHeaders();
        assertThat(headers.getFirst("X-Dev-User")).isNull();
        assertThat(headers.getFirst("adLogin")).isEqualTo("approver_login");
        assertThat(headers.getFirst("roles")).isEqualTo("EMPLOYEE,APPROVER");
        assertThat(headers.getFirst("channel")).isEqualTo("WEB");
        assertThat(headers.getFirst("sessionCreatedAt")).isEqualTo(NOW.toString());
        assertThatCodeIsUuid(headers.getFirst("sessionId"));
        assertThatCodeIsUuid(headers.getFirst("traceId"));
    }

    @Test
    void unknownProfileReturnsBadRequestAndDoesNotCallDownstream() {
        DevRouterProperties properties = DevUserResolverTest.properties();
        var filter = new DevUserHeadersFilter(
                properties,
                new DevUserResolver(properties),
                new ObjectMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api").header("X-Dev-User", "unknown").build()
        );
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, filteredExchange -> {
            forwarded.set(filteredExchange);
            return Mono.empty();
        }).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exchange.getResponse().getHeaders().getContentType().toString())
                .isEqualTo("application/json");
        assertThat(forwarded).hasValue(null);
    }

    @Test
    void reusesBrowserSessionIdFromHttpOnlyCookie() {
        DevRouterProperties properties = DevUserResolverTest.properties();
        var filter = new DevUserHeadersFilter(
                properties,
                new DevUserResolver(properties),
                new ObjectMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        String sessionId = UUID.randomUUID().toString();
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api")
                        .cookie(new org.springframework.http.HttpCookie("DEV_SESSION_ID", sessionId))
                        .build()
        );
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, filteredExchange -> {
            forwarded.set(filteredExchange);
            return Mono.empty();
        }).block();

        assertThat(forwarded.get().getRequest().getHeaders().getFirst("sessionId"))
                .isEqualTo(sessionId);
    }

    private static void assertThatCodeIsUuid(String value) {
        assertThat(value).isNotBlank();
        assertThat(UUID.fromString(value)).isNotNull();
    }
}
