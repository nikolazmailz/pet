package com.pet.requisition.application.model;
public record PreparedRequest<T>(RequestEnvelope<T> envelope,SystemParams systemParams,RequestRecord requestRecord,long startedAt) {
    public T payload(){return envelope.payload();}
    public String correlationId(){return envelope.correlationId();}
    public String requestType(){return envelope.requestType();}
}
