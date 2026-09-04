package ru.ntdev.srhr.requisition.pendingcandidates.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PendingCandidatesProperties.class)
public class PendingCandidatesConfiguration {
}
