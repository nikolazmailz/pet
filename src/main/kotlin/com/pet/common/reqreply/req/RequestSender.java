package com.pet.common.reqreply.req;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.ntdev.srhr.common.dto.api.request.SapRequestDto;
import ru.ntdev.srhr.common.redis.RedisAdapter;
import ru.ntdev.srhr.common.reqreply.dto.RequestEnvelope;

public class RequestSender {

    private final ObjectMapper objectMapper;
    private final RedisAdapter redisAdapter;
    private final Integer kafkaProducerMaxInnerMessageSizeKb;
    private final FilestorageService filestorageService;

    public RequestSender(ObjectMapper objectMapper,
                         RedisAdapter redisAdapter,
                         Integer kafkaProducerMaxInnerMessageSizeKb,
                         FilestorageService filestorageService) {
        this.objectMapper = objectMapper;
        this.redisAdapter = redisAdapter;
        this.kafkaProducerMaxInnerMessageSizeKb = kafkaProducerMaxInnerMessageSizeKb;
        this.filestorageService = filestorageService;
    }

    public <T> void send(RequestEnvelope<T> request, String queue) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(request);
        String checkedJson = checkMessageSize(request, json);
        redisAdapter.putToQueue(queue, checkedJson);
    }

    private <T> String checkMessageSize(RequestEnvelope<T> dto, String message) throws JsonProcessingException {
        int messageSizeKb = ((2 * message.length()) + 32) / 1000;
        if (messageSizeKb < kafkaProducerMaxInnerMessageSizeKb) {
            return message;
        }

        String messageUuid = filestorageService.putMessageToDb(
                dto.getAdLogin(),
                dto.getSessionId(),
                dto.getTraceId(),
                dto.getUuid(),
                dto.getAdLogin() + '_' + dto.getRequestType(),
                message);

        SapRequestDto storagedDto = new SapRequestDto();
        storagedDto.setRequestType(dto.getRequestType());
        storagedDto.setUuid(dto.getUuid());
        storagedDto.setAdLogin(dto.getAdLogin());
        storagedDto.setErrorMessage(dto.getErrorMessage());
        storagedDto.setChannel(dto.getChannel());
        storagedDto.setTraceId(dto.getTraceId());
        storagedDto.setSessionId(dto.getSessionId());
        storagedDto.setRealm(dto.getRealm());
        storagedDto.setFileUuid(messageUuid);

        return objectMapper.writeValueAsString(storagedDto);
    }
}
