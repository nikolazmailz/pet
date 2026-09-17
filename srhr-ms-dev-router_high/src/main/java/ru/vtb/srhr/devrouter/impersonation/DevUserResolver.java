package ru.vtb.srhr.devrouter.impersonation;

import org.springframework.http.HttpCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import ru.vtb.srhr.devrouter.config.DevRouterProperties;

import java.util.Map;

@Component
public class DevUserResolver {

    private final DevRouterProperties properties;

    public DevUserResolver(DevRouterProperties properties) {
        this.properties = properties;
    }

    public ResolvedDevUser resolve(ServerWebExchange exchange) {
        var settings = properties.getImpersonation();
        String headerValue = exchange.getRequest().getHeaders().getFirst(settings.getSelectorHeader());
        if (StringUtils.hasText(headerValue)) {
            return resolveById(headerValue.trim(), ResolvedDevUser.SelectionSource.HEADER);
        }

        HttpCookie cookie = exchange.getRequest().getCookies().getFirst(settings.getSelectorCookie());
        if (cookie != null && StringUtils.hasText(cookie.getValue())) {
            return resolveById(cookie.getValue().trim(), ResolvedDevUser.SelectionSource.COOKIE);
        }

        return resolveById(settings.getDefaultUser(), ResolvedDevUser.SelectionSource.DEFAULT);
    }

    public ResolvedDevUser resolveById(String userId, ResolvedDevUser.SelectionSource source) {
        DevRouterProperties.UserProfile profile = properties.getUsers().get(userId);
        if (profile == null) {
            throw new UnknownDevUserException(userId, properties.getUsers().keySet());
        }
        return new ResolvedDevUser(userId, profile, source);
    }

    public Map<String, DevRouterProperties.UserProfile> users() {
        return Map.copyOf(properties.getUsers());
    }
}
