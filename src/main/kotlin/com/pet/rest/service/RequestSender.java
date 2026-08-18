package com.pet.rest.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.requestreply.model.*;
import com.pet.rest.redis.*;

public class RequestSender {
    private final ObjectMapper objectMapper;
    private final RedisAdapter redisAdapter;
    private final String requestQueue;

    public RequestSender(ObjectMapper objectMapper, RedisAdapter redisAdapter, String requestQueue) {
        this.objectMapper = objectMapper;
        this.redisAdapter = redisAdapter;
        this.requestQueue = requestQueue;
    }

    public <T> void send(RequestEnvelope<T> request) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(request);
        redisAdapter.putToQueue(requestQueue, json);
    }
}
