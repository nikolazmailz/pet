package ru.ntdev.srhr.requisition.pendingcandidates.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * srhr:
 *   pending-candidates:
 *     master-data-integration-base-url: http://srhr-ms-master-data-integration:8080
 *     request-topic: srhr.requisition.to.easup
 *     reply-topic: srhr.requisition.from.easup
 */
@ConfigurationProperties(prefix = "srhr.pending-candidates")
public record PendingCandidatesProperties(
        String masterDataIntegrationBaseUrl,
        String requestTopic,
        String replyTopic) {
}
