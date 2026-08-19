package com.pet.requisition.application.model;

public record RequestEnvelope<T>(
        String uuid,
        String requestType,
        String sessionId,
        String adLogin,
        String traceId,
        String channel,
        String realm,
        String fileUuid,
        T payload
) {
    public RequestEnvelope(RequestEnvelope<?> envelope, T payload) {
        this(
                envelope.uuid(),
                envelope.requestType(),
                envelope.sessionId(),
                envelope.adLogin(),
                envelope.traceId(),
                envelope.channel(),
                envelope.realm(),
                envelope.fileUuid(),
                payload
        );
    }


}
