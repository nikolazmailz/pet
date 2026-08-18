package com.pet.rest.service;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.requestreply.model.*;
import com.pet.rest.redis.*;
import java.util.*;


public class RequestReplyService {
    private final RequestSender requestSender;
    private final RedisAdapter redisAdapter;
    private final ObjectMapper objectMapper;
    private final int timeoutLimit;
    private final long pollingDelayMs;

    public RequestReplyService(RequestSender requestSender,
                               RedisAdapter redisAdapter,
                               ObjectMapper objectMapper,
                               int timeoutLimit,
                               long pollingDelayMs) {
        this.requestSender = requestSender;
        this.redisAdapter = redisAdapter;
        this.objectMapper = objectMapper;
        this.timeoutLimit = timeoutLimit;
        this.pollingDelayMs = pollingDelayMs;
    }

    public <T, R> ResponseEnvelope<R> getResponse(RequestEnvelope<T> request, Class<R> responseClass) throws Exception {
        requestSender.send(request);
        return awaitResponse(request.correlationId(), responseClass);
    }

//    public String getGetResponse() throws Exception {
//        return objectMapper.writeValueAsString(getResponse(
//                new RequestEnvelope<>(Map.of("id", 1, "name", "Test")),
//                JsonNode.class
//        ));
//    }

    private <R> ResponseEnvelope<R> awaitResponse(String correlationId, Class<R> responseClass) throws Exception {
        int attempts = 0;
        JavaType envelopeType = objectMapper.getTypeFactory()
                .constructParametricType(ResponseEnvelope.class, JsonNode.class);

        while (attempts < timeoutLimit) {
            String json = redisAdapter.getStringFromDb(correlationId);
            if (json != null) {
                ResponseEnvelope<JsonNode> stored = objectMapper.readValue(json, envelopeType);
                R payload = stored.payload() == null || stored.payload().isNull()
                        ? null
                        : objectMapper.treeToValue(stored.payload(), responseClass);

                return new ResponseEnvelope<>(
                        stored.correlationId(),
                        stored.requestType(),
                        payload,
                        stored.error()
                );
            }

            attempts++;
            Thread.sleep(pollingDelayMs);
        }

        return null;
    }
}
