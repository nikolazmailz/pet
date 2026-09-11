package ru.ntdev.srhr.ms.mock.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(WireMockProperties.class)
public class WireMockConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer wireMockServer(WireMockProperties properties) {
        WireMockConfiguration configuration = WireMockConfiguration.options()
                .usingFilesUnderClasspath(properties.getRootDirectory())
                .notifier(new ConsoleNotifier(properties.isVerbose()));

        if (properties.getPort() == 0) {
            configuration.dynamicPort();
        } else {
            configuration.port(properties.getPort());
        }

        return new WireMockServer(configuration);
    }
}
