package ru.ntdev.srhr.devrouter.web;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.ntdev.srhr.devrouter.jwt.DevJwtProperties;
import ru.ntdev.srhr.devrouter.jwt.DevJwtService;
import ru.ntdev.srhr.devrouter.jwt.IssuedDevToken;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class DevTokenController {

    private final DevJwtService jwtService;

    public DevTokenController(DevJwtService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return jwtService.jwks();
    }

    /**
     * EPA-compatible alias used by WireMock proxy mapping on the DEV stand.
     */
    @GetMapping("/epa/open-keys")
    public Map<String, Object> epaOpenKeys() {
        return jwtService.jwks();
    }

    @GetMapping("/dev/jti")
    public Map<String, Boolean> jti() {
        return Map.of("status", true);
    }

    @GetMapping("/dev/users")
    public Map<String, Map<String, String>> users() {
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        jwtService.users().forEach((name, profile) -> result.put(name, publicProfile(profile)));
        return result;
    }

    @GetMapping("/dev/token")
    public TokenResponse token(
            @RequestParam(required = false) String user,
            ServerHttpRequest request
    ) {
        IssuedDevToken token = jwtService.issue(user, clientIp(request));
        long expiresIn = Math.max(0, Duration.between(Instant.now(), token.expiresAt()).toSeconds());
        return new TokenResponse(
                token.value(),
                "Bearer",
                expiresIn,
                token.user(),
                token.claims()
        );
    }

    private static Map<String, String> publicProfile(DevJwtProperties.UserProfile profile) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("sub", profile.getSub());
        result.put("ctxi", profile.getSessionId());
        result.put("channel", profile.getChannel());
        result.put("realm", profile.getRealm());
        return result;
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

    public record TokenResponse(
            String accessToken,
            String tokenType,
            long expiresIn,
            String user,
            Map<String, Object> claims
    ) {
    }
}
