package com.pet.requestreply.service;

import com.pet.requestreply.application.handler.*;
import com.pet.requestreply.application.model.*;
import com.pet.requestreply.application.port.out.*;

public class MessageProcessingService {
    private final MessageCodecPort codec;
    private final PreparedRequestService preparedRequestService;
    private final RequestHandlerExecutor executor;

    public MessageProcessingService(
            MessageCodecPort codec,
            PreparedRequestService preparedRequestService,
            RequestHandlerExecutor executor
    ) {
        this.codec = codec;
        this.preparedRequestService = preparedRequestService;
        this.executor = executor;
    }

    public String processMessage(String messageIncoming) throws Exception {
        RequestEnvelope<RawPayload> envelope = codec.deserializeRequest(messageIncoming);

        PreparedRequest<RawPayload> prepared =
                preparedRequestService.prepare(envelope, messageIncoming);

        ResponseEnvelope<?> response = executor.execute(prepared);

        return codec.serializeResponse(response);
    }
}
