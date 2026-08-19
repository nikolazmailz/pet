package com.pet.requisition.application.handler;


import com.pet.requisition.application.model.*;
import com.pet.requisition.application.port.out.*;

public class RequestHandlerExecutor {
    private final MessageCodecPort codec;
    private final RequestHandlerResolver resolver;

    public RequestHandlerExecutor(MessageCodecPort codec, RequestHandlerResolver resolver) {
        this.codec = codec;
        this.resolver = resolver;
    }

    public Object execute(PreparedRequest<RawPayload> prepared) throws Exception {
        return executeTyped(prepared, resolver.resolve(prepared.requestType()));
    }

    private <T, R> R executeTyped(PreparedRequest<RawPayload> prepared, RequestHandler<T, R> handler) throws Exception {
        T payload = prepared.payload() == null ? null
                : codec.deserializePayload(prepared.payload(), handler.requestClass());
        RequestEnvelope<T> requestEnvelope = new RequestEnvelope<>(prepared.envelope(), payload);
        PreparedRequest<T> typed = new PreparedRequest<>(requestEnvelope, prepared.systemParams(), prepared.requestRecord(), prepared.startedAt());
        return handler.handle(typed);
    }
}
