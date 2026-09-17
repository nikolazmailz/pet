package ru.vtb.srhr.devrouter.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import ru.vtb.srhr.devrouter.config.DevRouterProperties;
import ru.vtb.srhr.devrouter.impersonation.DevUserResolver;
import ru.vtb.srhr.devrouter.impersonation.ResolvedDevUser;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/dev/session")
@Profile("dev")
@ConditionalOnProperty(name = "dev-router.impersonation.enabled", havingValue = "true")
public class DevUserController {

    private final DevRouterProperties properties;
    private final DevUserResolver resolver;

    public DevUserController(DevRouterProperties properties, DevUserResolver resolver) {
        this.properties = properties;
        this.resolver = resolver;
    }

    @GetMapping("/users")
    public List<UserView> users() {
        return resolver.users().entrySet().stream()
                .map(entry -> new UserView(entry.getKey(), entry.getValue().getDisplayName()))
                .sorted((left, right) -> left.id().compareTo(right.id()))
                .toList();
    }

    @GetMapping("/current")
    public CurrentUserView current(ServerWebExchange exchange) {
        ResolvedDevUser user = resolver.resolve(exchange);
        return new CurrentUserView(user.id(), user.profile().getDisplayName(), user.source().name());
    }

    @PostMapping("/user/{userId}")
    public CurrentUserView select(@PathVariable String userId, ServerWebExchange exchange) {
        ResolvedDevUser user = resolver.resolveById(userId, ResolvedDevUser.SelectionSource.COOKIE);
        Duration maxAge = properties.getImpersonation().getCookieMaxAge();
        exchange.getResponse().addCookie(cookie(
                properties.getImpersonation().getSelectorCookie(),
                user.id(),
                maxAge
        ));
        exchange.getResponse().addCookie(cookie(
                properties.getImpersonation().getSessionCookie(),
                UUID.randomUUID().toString(),
                maxAge
        ));
        return new CurrentUserView(user.id(), user.profile().getDisplayName(), "COOKIE");
    }

    @DeleteMapping
    public Map<String, String> clear(ServerWebExchange exchange) {
        exchange.getResponse().addCookie(cookie(
                properties.getImpersonation().getSelectorCookie(),
                "",
                Duration.ZERO
        ));
        exchange.getResponse().addCookie(cookie(
                properties.getImpersonation().getSessionCookie(),
                "",
                Duration.ZERO
        ));
        return Map.of("status", "cleared");
    }

    private ResponseCookie cookie(String name, String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(properties.getImpersonation().isCookieSecure())
                .sameSite(properties.getImpersonation().getCookieSameSite())
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    public record UserView(String id, String displayName) {
    }

    public record CurrentUserView(String id, String displayName, String selectedBy) {
    }
}
