package ru.ntdev.srhr.devrouter.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import ru.ntdev.srhr.devrouter.jwt.DevJwtService;
import ru.ntdev.srhr.devrouter.jwt.IssuedDevToken;
import ru.ntdev.srhr.devrouter.jwt.UnknownDevUserException;
import ru.ntdev.srhr.devrouter.session.DevUserSelectionResolver;
import ru.ntdev.srhr.devrouter.session.SelectedDevUser;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class DevJwtInjectionFilter implements GlobalFilter, Ordered {

    public static final String DEV_USER_HEADER = "X-Dev-User";
    public static final String REQUEST_ID_HEADER = "X-Request-ID";

    private static final Logger log = LoggerFactory.getLogger(DevJwtInjectionFilter.class);

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
        if (isManagementPath(path)) {
            return chain.filter(exchange);
        }

        SelectedDevUser selectedUser;
        IssuedDevToken token;
        try {
            selectedUser = userSelectionResolver.resolve(exchange);
            token = jwtService.issue(
                    selectedUser.user(),
                    selectedUser.sessionId(),
                    clientIp(exchange.getRequest())
            );
        } catch (UnknownDevUserException e) {
            return writeBadRequest(exchange, e.getMessage());
        }

        String requestId = exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER);
        if (!StringUtils.hasText(requestId)) {
            requestId = UUID.randomUUID().toString();
        }

        String finalRequestId = requestId;
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    // A real corporate token must never leak through the DEV router.
                    headers.remove(HttpHeaders.AUTHORIZATION);
                    headers.setBearerAuth(token.value());
                    headers.set(DEV_USER_HEADER, token.user());
                    headers.set(REQUEST_ID_HEADER, finalRequestId);
                    headers.set("X-Dev-Router", "true");
                })
                .build();

        log.info("Proxy request: method={}, path={}, devUser={}, requestId={}",
                request.getMethod(), path, token.user(), requestId);

        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private static boolean isManagementPath(String path) {
        return path.startsWith("/actuator/")
                || path.startsWith("/dev/")
                || path.startsWith("/epa/")
                || path.equals("/dev-users.html")
                || path.equals("/.well-known/jwks.json");
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
