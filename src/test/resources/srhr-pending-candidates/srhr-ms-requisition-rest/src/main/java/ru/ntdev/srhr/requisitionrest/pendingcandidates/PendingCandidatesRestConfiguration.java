package ru.ntdev.srhr.requisitionrest.pendingcandidates;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PendingCandidatesRestProperties.class)
public class PendingCandidatesRestConfiguration {
}
