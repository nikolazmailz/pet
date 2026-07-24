package ru.ntdev.srhr.requisition.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "integration.master-data")
public record MasterDataIntegrationProperties(String baseUrl, Duration connectTimeout, Duration readTimeout) {
    public MasterDataIntegrationProperties {
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(2) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(15) : readTimeout;
    }
}
