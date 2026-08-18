// Пример конфигурации в конкретном сервисе (не в common).
// Пакет поменяй под свой сервис.
package com.pet.common.reqreply;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.ntdev.srhr.common.redis.RedisAdapter;
import ru.ntdev.srhr.common.reqreply.req.FilestorageRestClient;
import ru.ntdev.srhr.common.reqreply.req.FilestorageService;
import ru.ntdev.srhr.common.reqreply.req.FilestorageServiceImpl;
import ru.ntdev.srhr.common.reqreply.req.RequestSender;

@Configuration
public class ReqReplyConfig {

    @Bean
    public FilestorageRestClient filestorageRestClient(
            ObjectMapper objectMapper,
            @Value("${filestorage.rest.service.url}") String filestorageUrl) {
        // Если нужен IntegrationRequestInterceptor — передай его третьим аргументом:
        // return new FilestorageRestClient(objectMapper, filestorageUrl, List.of(new IntegrationRequestInterceptor()));
        return new FilestorageRestClient(objectMapper, filestorageUrl);
    }

    @Bean
    public FilestorageService filestorageService(FilestorageRestClient restClient) {
        return new FilestorageServiceImpl(restClient);
    }

    @Bean
    public RequestSender requestSender(
            ObjectMapper objectMapper,
            RedisAdapter redisAdapter,
            @Value("${kafka.producer.max-inner-message-size-kb}") Integer maxInnerMessageSizeKb,
            FilestorageService filestorageService) {
        return new RequestSender(objectMapper, redisAdapter, maxInnerMessageSizeKb, filestorageService);
    }
}
