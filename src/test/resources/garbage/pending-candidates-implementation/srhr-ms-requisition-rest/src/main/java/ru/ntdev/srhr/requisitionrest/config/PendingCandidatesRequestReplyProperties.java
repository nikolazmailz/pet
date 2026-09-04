package ru.ntdev.srhr.requisitionrest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "pending-candidates.request-reply")
public record PendingCandidatesRequestReplyProperties(
        Duration timeout,
        Duration responseTtl,
        String responseKeyPrefix,
        String notificationChannel
) {
    public PendingCandidatesRequestReplyProperties {
        timeout = timeout == null ? Duration.ofSeconds(20) : timeout;
        responseTtl = responseTtl == null ? Duration.ofMinutes(1) : responseTtl;
        responseKeyPrefix = responseKeyPrefix == null ? "pending-candidates:response:" : responseKeyPrefix;
        notificationChannel = notificationChannel == null ? "pending-candidates:response-notifications" : notificationChannel;
    }
}
