package ru.ntdev.srhr.masterdataintegration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * estaff:
 *   base-url: http://sappo.internal/estaff
 */
@ConfigurationProperties(prefix = "estaff")
public record EstaffProperties(String baseUrl) {
}
