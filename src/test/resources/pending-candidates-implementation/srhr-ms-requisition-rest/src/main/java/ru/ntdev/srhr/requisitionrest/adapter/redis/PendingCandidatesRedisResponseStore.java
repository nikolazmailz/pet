package ru.ntdev.srhr.requisitionrest.adapter.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import ru.ntdev.srhr.pending.contracts.PendingCandidatesKafkaResponse;
import ru.ntdev.srhr.requisitionrest.config.PendingCandidatesRequestReplyProperties;

@Component
public class PendingCandidatesRedisResponseStore {
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final PendingCandidatesRequestReplyProperties properties;

    public PendingCandidatesRedisResponseStore(StringRedisTemplate redis,
                                                ObjectMapper objectMapper,
                                                PendingCandidatesRequestReplyProperties properties) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public void saveAndNotify(PendingCandidatesKafkaResponse response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redis.opsForValue().set(key(response.correlationId()), json, properties.responseTtl());
            redis.convertAndSend(properties.notificationChannel(), response.correlationId());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Не удалось сериализовать Kafka-ответ", ex);
        }
    }

    public PendingCandidatesKafkaResponse get(String correlationId) {
        String json = redis.opsForValue().get(key(correlationId));
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, PendingCandidatesKafkaResponse.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Не удалось десериализовать ответ из Redis", ex);
        }
    }

    private String key(String correlationId) {
        return properties.responseKeyPrefix() + correlationId;
    }
}
