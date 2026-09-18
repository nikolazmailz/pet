package ru.ntdev.srhr.devgw.session;

import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import ru.ntdev.srhr.devgw.jwt.DevJwtService;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/dev/session")
public class DevSessionController {

    private final DevJwtService jwtService;
    private final DevUserSelectionResolver userSelectionResolver;
    private final DevSessionProperties sessionProperties;

    public DevSessionController(
            DevJwtService jwtService,
            DevUserSelectionResolver userSelectionResolver,
            DevSessionProperties sessionProperties
    ) {
        this.jwtService = jwtService;
        this.userSelectionResolver = userSelectionResolver;
        this.sessionProperties = sessionProperties;
    }

    @GetMapping("/users")
    public List<UserView> users() {
        return jwtService.users().entrySet().stream()
                .map(entry -> new UserView(entry.getKey(), entry.getValue().getSub()))
                .sorted((left, right) -> left.id().compareTo(right.id()))
                .toList();
    }

    @GetMapping("/current")
    public CurrentUserView current(ServerWebExchange exchange) {
        return view(userSelectionResolver.resolve(exchange));
    }

    @PostMapping("/user/{user}")
    public CurrentUserView select(@PathVariable String user, ServerWebExchange exchange) {
        String sessionId = UUID.randomUUID().toString();
        SelectedDevUser selected = userSelectionResolver.selected(
                user,
                sessionId,
                SelectedDevUser.SelectionSource.COOKIE
        );
        Duration maxAge = sessionProperties.getCookieMaxAge();
        exchange.getResponse().addCookie(cookie(sessionProperties.getUserCookie(), user, maxAge));
        exchange.getResponse().addCookie(cookie(sessionProperties.getSessionCookie(), sessionId, maxAge));
        return view(selected);
    }

    @DeleteMapping
    public Map<String, String> clear(ServerWebExchange exchange) {
        exchange.getResponse().addCookie(cookie(sessionProperties.getUserCookie(), "", Duration.ZERO));
        exchange.getResponse().addCookie(cookie(sessionProperties.getSessionCookie(), "", Duration.ZERO));
        return Map.of("status", "cleared");
    }

    private CurrentUserView view(SelectedDevUser selected) {
        return new CurrentUserView(
                selected.user(),
                jwtService.users().get(selected.user()).getSub(),
                selected.sessionId(),
                selected.selectedBy().name()
        );
    }

    private ResponseCookie cookie(String name, String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(sessionProperties.isCookieSecure())
                .sameSite(sessionProperties.getCookieSameSite())
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    public record UserView(String id, String subject) {
    }

    public record CurrentUserView(
            String id,
            String subject,
            String sessionId,
            String selectedBy
    ) {
    }
}
