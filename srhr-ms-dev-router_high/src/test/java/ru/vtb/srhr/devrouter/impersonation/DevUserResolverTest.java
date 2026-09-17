package ru.vtb.srhr.devrouter.impersonation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import ru.vtb.srhr.devrouter.config.DevRouterProperties;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DevUserResolverTest {

    private DevUserResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new DevUserResolver(properties());
    }

    @Test
    void selectorHeaderHasPriorityOverCookie() {
        var request = MockServerHttpRequest.get("/api")
                .header("X-Dev-User", "approver")
                .cookie(new org.springframework.http.HttpCookie("DEV_USER", "employee"))
                .build();

        ResolvedDevUser result = resolver.resolve(MockServerWebExchange.from(request));

        assertThat(result.id()).isEqualTo("approver");
        assertThat(result.source()).isEqualTo(ResolvedDevUser.SelectionSource.HEADER);
    }

    @Test
    void cookieIsUsedWhenHeaderIsMissing() {
        var request = MockServerHttpRequest.get("/api")
                .cookie(new org.springframework.http.HttpCookie("DEV_USER", "approver"))
                .build();

        ResolvedDevUser result = resolver.resolve(MockServerWebExchange.from(request));

        assertThat(result.id()).isEqualTo("approver");
        assertThat(result.source()).isEqualTo(ResolvedDevUser.SelectionSource.COOKIE);
    }

    @Test
    void defaultUserIsUsedWhenNoSelectorWasProvided() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api").build());

        ResolvedDevUser result = resolver.resolve(exchange);

        assertThat(result.id()).isEqualTo("employee");
        assertThat(result.source()).isEqualTo(ResolvedDevUser.SelectionSource.DEFAULT);
    }

    @Test
    void unknownUserIsRejected() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api").header("X-Dev-User", "root").build()
        );

        assertThatThrownBy(() -> resolver.resolve(exchange))
                .isInstanceOf(UnknownDevUserException.class)
                .hasMessageContaining("root");
    }

    static DevRouterProperties properties() {
        var properties = new DevRouterProperties();
        properties.getImpersonation().setEnabled(true);
        properties.getImpersonation().setDefaultUser("employee");

        Map<String, DevRouterProperties.UserProfile> users = new LinkedHashMap<>();
        users.put("employee", profile("Employee", "employee_login", "EMPLOYEE"));
        users.put("approver", profile("Approver", "approver_login", "EMPLOYEE,APPROVER"));
        properties.setUsers(users);
        return properties;
    }

    private static DevRouterProperties.UserProfile profile(
            String displayName,
            String adLogin,
            String roles
    ) {
        var profile = new DevRouterProperties.UserProfile();
        profile.setDisplayName(displayName);
        profile.setHeaders(new LinkedHashMap<>(Map.of(
                "adLogin", adLogin,
                "roles", roles,
                "channel", "WEB"
        )));
        return profile;
    }
}
