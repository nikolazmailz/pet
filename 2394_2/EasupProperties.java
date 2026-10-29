package ru.ntdev.srhr.ms.easup.integration.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Биндится на существующие ключи application.yml без изменений:
 * <pre>
 * easup:
 *   ssl:
 *     mss-full: { trust-store, trust-store-password, trust-store-type, key-store, key-store-password, key-store-type }
 *     mss-limited: ...
 *   basic-auth: { enable, login, password }
 *   headers: { service-id-header-name, service-id-header-staff-value, service-id-header-staff-mobile-value }
 * </pre>
 * {@code @Component} можно убрать, если на main-классе стоит {@code @ConfigurationPropertiesScan}.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "easup")
public class EasupProperties {

    /** Ключ — имя стора из yml: mss-full, mss-limited, mss-standard, ess, tabel. */
    private Map<String, SslStores> ssl = new HashMap<>();
    private BasicAuth basicAuth = new BasicAuth();
    private Headers headers = new Headers();

    @Getter
    @Setter
    public static class SslStores {
        private String trustStore;
        private String trustStorePassword;
        private String trustStoreType;
        private String keyStore;
        private String keyStorePassword;
        private String keyStoreType;
    }

    @Getter
    @Setter
    public static class BasicAuth {
        private boolean enable;
        private String login;
        private String password;
    }

    @Getter
    @Setter
    public static class Headers {
        private String serviceIdHeaderName;
        private String serviceIdHeaderStaffValue;
        private String serviceIdHeaderStaffMobileValue;
    }
}
