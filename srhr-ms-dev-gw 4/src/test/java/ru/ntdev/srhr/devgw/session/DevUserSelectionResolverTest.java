package ru.ntdev.srhr.devgw.session;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpCookie;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import ru.ntdev.srhr.devgw.jwt.DevJwtProperties;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DevUserSelectionResolverTest {

    @Test
    void headerHasPriorityOverCookie() {
        DevUserSelectionResolver resolver = resolver();
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

    private static DevUserSelectionResolver resolver() {
        DevJwtProperties properties = new DevJwtProperties();
        properties.setDefaultUser("employee");
        properties.setUsers(Map.of(
                "employee", profile("employee@VTB.RU", "employee-session"),
                "approver", profile("approver@VTB.RU", "approver-session")
        ));
        return new DevUserSelectionResolver(properties, new DevSessionProperties());
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
