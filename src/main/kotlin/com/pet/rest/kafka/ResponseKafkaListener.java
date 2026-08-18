package com.pet.rest.kafka;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.requestreply.model.*;
import com.pet.rest.redis.RedisAdapter;

public class ResponseKafkaListener {
    private final ObjectMapper objectMapper;
    private final RedisAdapter redisAdapter;
    private final int ttlSeconds;

    public ResponseKafkaListener(ObjectMapper objectMapper, RedisAdapter redisAdapter, int ttlSeconds) {
        this.objectMapper = objectMapper;
        this.redisAdapter = redisAdapter;
        this.ttlSeconds = ttlSeconds;
    }

    public void listen(String data) throws Exception {
        JavaType type = objectMapper.getTypeFactory()
                .constructParametricType(ResponseEnvelope.class, JsonNode.class);

        ResponseEnvelope<JsonNode> response = objectMapper.readValue(data, type);
        redisAdapter.putStringToDb(response.correlationId(), data, ttlSeconds);
    }
}
