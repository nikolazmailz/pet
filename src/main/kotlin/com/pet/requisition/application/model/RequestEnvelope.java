package com.pet.requisition.application.model;
public record RequestEnvelope<T>(
        String correlationId,
        String requestType,
        String sessionId,
        String adLogin,
        String traceId,
        String channel,
        String realm,
        String fileUuid,
        T payload
) {}
