package ru.ntdev.srhr.devgw.session;

import org.springframework.http.HttpCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import ru.ntdev.srhr.devgw.jwt.DevJwtProperties;
import ru.ntdev.srhr.devgw.jwt.UnknownDevUserException;

@Component
public class DevUserSelectionResolver {

    private final DevJwtProperties jwtProperties;
    private final DevSessionProperties sessionProperties;

    public DevUserSelectionResolver(
            DevJwtProperties jwtProperties,
            DevSessionProperties sessionProperties
    ) {
        this.jwtProperties = jwtProperties;
        this.sessionProperties = sessionProperties;
    }

    public SelectedDevUser resolve(ServerWebExchange exchange) {
        String headerUser = exchange.getRequest().getHeaders()
                .getFirst(sessionProperties.getSelectorHeader());
        if (StringUtils.hasText(headerUser)) {
            return selected(headerUser.trim(), sessionId(exchange), SelectedDevUser.SelectionSource.HEADER);
        }

        HttpCookie userCookie = exchange.getRequest().getCookies()
                .getFirst(sessionProperties.getUserCookie());
        if (userCookie != null && StringUtils.hasText(userCookie.getValue())) {
            return selected(userCookie.getValue().trim(), sessionId(exchange), SelectedDevUser.SelectionSource.COOKIE);
        }

        return selected(
                jwtProperties.getDefaultUser(),
                sessionId(exchange),
                SelectedDevUser.SelectionSource.DEFAULT
        );
    }

    public SelectedDevUser selected(
            String user,
            String sessionId,
            SelectedDevUser.SelectionSource source
    ) {
        DevJwtProperties.UserProfile profile = jwtProperties.getUsers().get(user);
        if (profile == null) {
            throw new UnknownDevUserException(user);
        }
        return new SelectedDevUser(
                user,
                StringUtils.hasText(sessionId) ? sessionId : profile.getSessionId(),
                source
        );
    }

    private String sessionId(ServerWebExchange exchange) {
        HttpCookie cookie = exchange.getRequest().getCookies()
                .getFirst(sessionProperties.getSessionCookie());
        return cookie != null && StringUtils.hasText(cookie.getValue())
                ? cookie.getValue().trim()
                : null;
    }
}
