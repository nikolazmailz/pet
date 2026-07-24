package ru.ntdev.srhr.requisitionrest.pendingcandidates;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * srhr:
 *   pending-candidates:
 *     request-topic: srhr.requisition.to.easup
 *     reply-timeout: 30s
 */
@ConfigurationProperties(prefix = "srhr.pending-candidates")
public record PendingCandidatesRestProperties(String requestTopic, Duration replyTimeout) {
}
