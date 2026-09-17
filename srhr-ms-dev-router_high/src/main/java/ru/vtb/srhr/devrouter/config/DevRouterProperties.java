package ru.vtb.srhr.devrouter.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Validated
@ConfigurationProperties(prefix = "dev-router")
public class DevRouterProperties {

    @Valid
    private Impersonation impersonation = new Impersonation();

    @Valid
    private Map<String, UserProfile> users = new LinkedHashMap<>();

    @AssertTrue(message = "when impersonation is enabled, users must contain the configured default-user")
    public boolean isImpersonationConfigurationValid() {
        return !impersonation.isEnabled()
                || (users != null
                && !users.isEmpty()
                && users.containsKey(impersonation.getDefaultUser()));
    }

    public Impersonation getImpersonation() {
        return impersonation;
    }

    public void setImpersonation(Impersonation impersonation) {
        this.impersonation = impersonation;
    }

    public Map<String, UserProfile> getUsers() {
        return users;
    }

    public void setUsers(Map<String, UserProfile> users) {
        this.users = users;
    }

    public static class Impersonation {

        private boolean enabled;

        @NotBlank
        private String selectorHeader = "X-Dev-User";

        @NotBlank
        private String selectorCookie = "DEV_USER";

        @NotBlank
        private String sessionCookie = "DEV_SESSION_ID";

        @NotBlank
        private String defaultUser = "employee";

        @NotBlank
        private String sessionHeader = "sessionId";

        @NotBlank
        private String traceHeader = "traceId";

        @NotBlank
        private String sessionCreatedAtHeader = "sessionCreatedAt";

        private Duration cookieMaxAge = Duration.ofHours(8);

        private boolean cookieSecure;

        @NotBlank
        private String cookieSameSite = "Lax";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getSelectorHeader() {
            return selectorHeader;
        }

        public void setSelectorHeader(String selectorHeader) {
            this.selectorHeader = selectorHeader;
        }

        public String getSelectorCookie() {
            return selectorCookie;
        }

        public void setSelectorCookie(String selectorCookie) {
            this.selectorCookie = selectorCookie;
        }

        public String getSessionCookie() {
            return sessionCookie;
        }

        public void setSessionCookie(String sessionCookie) {
            this.sessionCookie = sessionCookie;
        }

        public String getDefaultUser() {
            return defaultUser;
        }

        public void setDefaultUser(String defaultUser) {
            this.defaultUser = defaultUser;
        }

        public String getSessionHeader() {
            return sessionHeader;
        }

        public void setSessionHeader(String sessionHeader) {
            this.sessionHeader = sessionHeader;
        }

        public String getTraceHeader() {
            return traceHeader;
        }

        public void setTraceHeader(String traceHeader) {
            this.traceHeader = traceHeader;
        }

        public String getSessionCreatedAtHeader() {
            return sessionCreatedAtHeader;
        }

        public void setSessionCreatedAtHeader(String sessionCreatedAtHeader) {
            this.sessionCreatedAtHeader = sessionCreatedAtHeader;
        }

        public Duration getCookieMaxAge() {
            return cookieMaxAge;
        }

        public void setCookieMaxAge(Duration cookieMaxAge) {
            this.cookieMaxAge = cookieMaxAge;
        }

        public boolean isCookieSecure() {
            return cookieSecure;
        }

        public void setCookieSecure(boolean cookieSecure) {
            this.cookieSecure = cookieSecure;
        }

        public String getCookieSameSite() {
            return cookieSameSite;
        }

        public void setCookieSameSite(String cookieSameSite) {
            this.cookieSameSite = cookieSameSite;
        }
    }

    public static class UserProfile {

        @NotBlank
        private String displayName;

        @NotEmpty
        private Map<String, String> headers = new LinkedHashMap<>();

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }
    }
}
