package com.pet.requisition.application.port.out;


import com.pet.requisition.application.model.*;

public interface MessageCodecPort {
    RequestEnvelope<RawPayload> deserializeRequest(String message) throws Exception;

    <T> T deserializePayload(RawPayload payload, Class<T> targetClass) throws Exception;

    String serializeResponse(ResponseEnvelope<?> response) throws Exception;

    String serializeObject(Object value) throws Exception;
}
