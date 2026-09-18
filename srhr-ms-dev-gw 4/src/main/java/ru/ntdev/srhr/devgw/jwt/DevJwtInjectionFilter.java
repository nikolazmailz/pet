package ru.ntdev.srhr.devgw.jwt;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.ntdev.srhr.devgw.session.DevUserSelectionResolver;
import ru.ntdev.srhr.devgw.session.SelectedDevUser;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

@Component
public class DevJwtInjectionFilter implements GlobalFilter, Ordered {

    private final DevJwtService jwtService;
    private final DevUserSelectionResolver userSelectionResolver;

    public DevJwtInjectionFilter(
            DevJwtService jwtService,
            DevUserSelectionResolver userSelectionResolver
    ) {
        this.jwtService = jwtService;
        this.userSelectionResolver = userSelectionResolver;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (path.startsWith("/actuator")
                || path.startsWith("/dev/")
                || path.equals("/dev-users.html")
                || path.equals("/.well-known/jwks.json")) {
            return chain.filter(exchange);
        }

        SelectedDevUser selected;
        String jwt;
        try {
            selected = userSelectionResolver.resolve(exchange);
            jwt = jwtService.issue(
                    selected.user(),
                    selected.sessionId(),
                    clientIp(exchange.getRequest())
            );
        } catch (UnknownDevUserException e) {
            return writeBadRequest(exchange, e.getMessage());
        }

        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(HttpHeaders.AUTHORIZATION);
                    headers.setBearerAuth(jwt);
                    headers.set("X-Dev-User", selected.user());
                    headers.set("X-Dev-Gateway", "true");
                })
                .build();

        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private static String clientIp(ServerHttpRequest request) {
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        return remoteAddress != null && remoteAddress.getAddress() != null
                ? remoteAddress.getAddress().getHostAddress()
                : "127.0.0.1";
    }

    private static Mono<Void> writeBadRequest(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String safeMessage = message.replace("\\", "\\\\").replace("\"", "\\\"");
        byte[] bytes = ("{\"error\":\"" + safeMessage + "\"}").getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
