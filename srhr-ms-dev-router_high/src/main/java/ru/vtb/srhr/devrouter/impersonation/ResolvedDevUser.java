package ru.vtb.srhr.devrouter.impersonation;

import ru.vtb.srhr.devrouter.config.DevRouterProperties;

public record ResolvedDevUser(
        String id,
        DevRouterProperties.UserProfile profile,
        SelectionSource source
) {
    public enum SelectionSource {
        HEADER,
        COOKIE,
        DEFAULT
    }
}
