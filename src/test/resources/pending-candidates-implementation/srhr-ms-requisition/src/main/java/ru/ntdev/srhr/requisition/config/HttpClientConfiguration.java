package ru.ntdev.srhr.requisition.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfiguration {
    @Bean
    RestClient masterDataRestClient(RestClient.Builder builder, MasterDataIntegrationProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) properties.connectTimeout().toMillis());
        factory.setReadTimeout((int) properties.readTimeout().toMillis());
        return builder.baseUrl(properties.baseUrl()).requestFactory(factory).build();
    }
}
