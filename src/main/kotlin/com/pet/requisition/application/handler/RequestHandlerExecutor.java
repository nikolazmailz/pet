package com.pet.requisition.application.handler;
import ru.ntdev.srhr.ms.requisition.application.model.*;
import ru.ntdev.srhr.ms.requisition.application.port.out.MessageCodecPort;
public class RequestHandlerExecutor {
    private final MessageCodecPort codec; private final RequestHandlerResolver resolver;
    public RequestHandlerExecutor(MessageCodecPort codec,RequestHandlerResolver resolver){this.codec=codec;this.resolver=resolver;}
    public Object execute(PreparedRequest<RawPayload> prepared) throws Exception { return executeTyped(prepared,resolver.resolve(prepared.requestType())); }
    private <T,R> R executeTyped(PreparedRequest<RawPayload> prepared,RequestHandler<T,R> handler) throws Exception {
        T payload = prepared.payload()==null ? null : codec.deserializePayload(prepared.payload(),handler.requestClass());
        RequestEnvelope<T> e=new RequestEnvelope<>(prepared.envelope().correlationId(),prepared.envelope().requestType(),prepared.envelope().sessionId(),prepared.envelope().adLogin(),prepared.envelope().traceId(),prepared.envelope().channel(),prepared.envelope().realm(),prepared.envelope().fileUuid(),payload);
        PreparedRequest<T> typed=new PreparedRequest<>(e,prepared.systemParams(),prepared.requestRecord(),prepared.startedAt());
        return handler.handle(typed);
    }
}
