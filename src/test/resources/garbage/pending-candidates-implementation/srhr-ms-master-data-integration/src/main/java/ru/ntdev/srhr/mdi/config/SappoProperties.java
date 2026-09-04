package ru.ntdev.srhr.mdi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "integration.sappo")
public record SappoProperties(
        String baseUrl,
        Duration connectTimeout,
        Duration readTimeout
) {
    public SappoProperties {
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(2) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(10) : readTimeout;
    }
}
