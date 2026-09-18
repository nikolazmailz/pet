package ru.ntdev.srhr.devrouter.session;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpCookie;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import ru.ntdev.srhr.devrouter.jwt.DevJwtProperties;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DevUserSelectionResolverTest {

    @Test
    void headerHasPriorityOverCookieAndSessionCookieBecomesCtxi() {
        DevJwtProperties jwtProperties = properties();
        DevSessionProperties sessionProperties = new DevSessionProperties();
        DevUserSelectionResolver resolver = new DevUserSelectionResolver(jwtProperties, sessionProperties);
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api")
                        .header("X-Dev-User", "approver")
                        .cookie(new HttpCookie("DEV_USER", "employee"))
                        .cookie(new HttpCookie("DEV_SESSION_ID", "browser-session"))
                        .build()
        );

        SelectedDevUser selected = resolver.resolve(exchange);

        assertThat(selected.user()).isEqualTo("approver");
        assertThat(selected.sessionId()).isEqualTo("browser-session");
        assertThat(selected.selectedBy()).isEqualTo(SelectedDevUser.SelectionSource.HEADER);
    }

    @Test
    void cookieSelectsUserForBrowserWithoutFrontendChanges() {
        DevJwtProperties jwtProperties = properties();
        DevUserSelectionResolver resolver = new DevUserSelectionResolver(
                jwtProperties,
                new DevSessionProperties()
        );
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api")
                        .cookie(new HttpCookie("DEV_USER", "approver"))
                        .cookie(new HttpCookie("DEV_SESSION_ID", "browser-session"))
                        .build()
        );

        SelectedDevUser selected = resolver.resolve(exchange);

        assertThat(selected.user()).isEqualTo("approver");
        assertThat(selected.selectedBy()).isEqualTo(SelectedDevUser.SelectionSource.COOKIE);
    }

    private static DevJwtProperties properties() {
        DevJwtProperties properties = new DevJwtProperties();
        properties.setDefaultUser("employee");
        properties.setUsers(Map.of(
                "employee", profile("employee@VTB.RU", "employee-session"),
                "approver", profile("approver@VTB.RU", "approver-session")
        ));
        return properties;
    }

    private static DevJwtProperties.UserProfile profile(String sub, String sessionId) {
        DevJwtProperties.UserProfile profile = new DevJwtProperties.UserProfile();
        profile.setSub(sub);
        profile.setSessionId(sessionId);
        profile.setChannel("WEB");
        profile.setRealm("SRHR");
        return profile;
    }
}
