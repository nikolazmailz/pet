package com.pet.requisition.application.model;

public record PreparedRequest<T>(
        RequestEnvelope<T> envelope,
        SystemParams systemParams,
        RequestRecord requestRecord,
        long startedAt
) {
    public T payload() {
        return envelope.payload();
    }

    public String correlationId() {
        return envelope.uuid();
    }

    public String requestType() {
        return envelope.requestType();
    }
}
