package com.pet.requestreply.model;

public record PreparedRequest<T>(
        RequestEnvelope<T> envelope,
//        SapSystemParamsDto systemParams,
//        RequisitionRequestEntity requestEntity,
        long startedAt
) {

    public T payload() {
        return envelope.payload();
    }

    public String correlationId() {
        return envelope.correlationId();
    }

    public String requestType() {
        return envelope.requestType();
    }
}