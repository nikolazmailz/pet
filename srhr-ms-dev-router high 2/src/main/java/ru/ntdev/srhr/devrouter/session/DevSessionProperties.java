package ru.ntdev.srhr.devrouter.session;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "dev-router.session")
public class DevSessionProperties {

    private String selectorHeader = "X-Dev-User";
    private String userCookie = "DEV_USER";
    private String sessionCookie = "DEV_SESSION_ID";
    private Duration cookieMaxAge = Duration.ofHours(8);
    private boolean cookieSecure;
    private String cookieSameSite = "Lax";

    public String getSelectorHeader() {
        return selectorHeader;
    }

    public void setSelectorHeader(String selectorHeader) {
        this.selectorHeader = selectorHeader;
    }

    public String getUserCookie() {
        return userCookie;
    }

    public void setUserCookie(String userCookie) {
        this.userCookie = userCookie;
    }

    public String getSessionCookie() {
        return sessionCookie;
    }

    public void setSessionCookie(String sessionCookie) {
        this.sessionCookie = sessionCookie;
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
