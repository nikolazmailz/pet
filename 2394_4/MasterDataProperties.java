package ru.ntdev.srhr.ms.sys.delegation.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Биндится на существующие ключи application.yml:
 * <pre>
 * master-data:
 *   ssl: { trust-store, trust-store-password, trust-store-type, key-store, key-store-password, key-store-type }
 *   headers: { service-id-header-name, service-id-header-staff-value, service-id-header-staff-mobile-value }
 * </pre>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "master-data")
public class MasterDataProperties {

    private Ssl ssl = new Ssl();
    private Headers headers = new Headers();

    @Getter
    @Setter
    public static class Ssl {
        private String trustStore;
        private String trustStorePassword;
        private String trustStoreType;
        private String keyStore;
        private String keyStorePassword;
        private String keyStoreType;
    }

    @Getter
    @Setter
    public static class Headers {
        private String serviceIdHeaderName;
        private String serviceIdHeaderStaffValue;
        private String serviceIdHeaderStaffMobileValue;
    }
}
