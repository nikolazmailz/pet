package ru.ntdev.srhr.masterdataintegration.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(EstaffProperties.class)
public class EstaffConfiguration {
}
