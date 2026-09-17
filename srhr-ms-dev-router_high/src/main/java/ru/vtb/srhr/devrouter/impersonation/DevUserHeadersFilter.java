package ru.vtb.srhr.devrouter.impersonation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.vtb.srhr.devrouter.config.DevRouterProperties;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@Profile("dev")
@ConditionalOnProperty(name = "dev-router.impersonation.enabled", havingValue = "true")
public class DevUserHeadersFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(DevUserHeadersFilter.class);

    private final DevRouterProperties properties;
    private final DevUserResolver resolver;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public DevUserHeadersFilter(
            DevRouterProperties properties,
            DevUserResolver resolver,
            ObjectMapper objectMapper
    ) {
        this(properties, resolver, objectMapper, Clock.systemUTC());
    }

    DevUserHeadersFilter(
            DevRouterProperties properties,
            DevUserResolver resolver,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.properties = properties;
        this.resolver = resolver;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith("/dev/") || path.startsWith("/actuator/")) {
            return chain.filter(exchange);
        }

        try {
            ResolvedDevUser user = resolver.resolve(exchange);
            Set<String> protectedHeaders = protectedHeaders();
            var impersonation = properties.getImpersonation();
            Instant now = clock.instant();
            String sessionId = sessionId(exchange);

            var request = exchange.getRequest().mutate().headers(headers -> {
                protectedHeaders.forEach(headers::remove);
                headers.remove(impersonation.getSelectorHeader());
                user.profile().getHeaders().forEach(headers::set);
                headers.set(impersonation.getSessionHeader(), sessionId);
                headers.set(impersonation.getTraceHeader(), UUID.randomUUID().toString());
                headers.set(impersonation.getSessionCreatedAtHeader(), now.toString());
            }).build();

            log.debug("DEV request {} {} uses user '{}' selected by {}",
                    request.getMethod(), path, user.id(), user.source());

            return chain.filter(exchange.mutate().request(request).build());
        } catch (UnknownDevUserException exception) {
            return writeUnknownUser(exchange, exception);
        }
    }

    private String sessionId(ServerWebExchange exchange) {
        HttpCookie cookie = exchange.getRequest().getCookies()
                .getFirst(properties.getImpersonation().getSessionCookie());
        if (cookie != null && !cookie.getValue().isBlank()) {
            return cookie.getValue();
        }
        return UUID.randomUUID().toString();
    }

    private Set<String> protectedHeaders() {
        Set<String> result = new LinkedHashSet<>();
        properties.getUsers().values().forEach(profile -> result.addAll(profile.getHeaders().keySet()));
        result.add(properties.getImpersonation().getSessionHeader());
        result.add(properties.getImpersonation().getTraceHeader());
        result.add(properties.getImpersonation().getSessionCreatedAtHeader());
        return result;
    }

    private Mono<Void> writeUnknownUser(ServerWebExchange exchange, UnknownDevUserException exception) {
        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "UNKNOWN_DEV_USER");
        body.put("message", exception.getMessage());
        body.put("availableUsers", exception.getAvailableUsers());

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException ignored) {
            bytes = "{\"code\":\"UNKNOWN_DEV_USER\"}".getBytes(StandardCharsets.UTF_8);
        }
        exchange.getResponse().getHeaders().setContentLength(bytes.length);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}
