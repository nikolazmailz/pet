package ru.ntdev.srhr.devgw.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "dev-gateway.jwt")
public class DevJwtProperties {

    private String issuer = "srhr-ms-dev-gw";
    private String audience = "srhr-dev";
    private String keyId = "srhr-dev-gw-key";
    private Duration ttl = Duration.ofHours(1);
    private String defaultUser = "employee";
    private Map<String, UserProfile> users = new LinkedHashMap<>();

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }

    public String getDefaultUser() {
        return defaultUser;
    }

    public void setDefaultUser(String defaultUser) {
        this.defaultUser = defaultUser;
    }

    public Map<String, UserProfile> getUsers() {
        return users;
    }

    public void setUsers(Map<String, UserProfile> users) {
        this.users = users;
    }

    public static class UserProfile {

        private String sub;
        private String sessionId;
        private String channel;
        private String realm;

        public String getSub() {
            return sub;
        }

        public void setSub(String sub) {
            this.sub = sub;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getChannel() {
            return channel;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        public String getRealm() {
            return realm;
        }

        public void setRealm(String realm) {
            this.realm = realm;
        }
    }
}
