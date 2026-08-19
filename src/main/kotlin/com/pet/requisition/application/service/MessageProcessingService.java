package com.pet.requisition.application.service;


import com.pet.requisition.application.exception.*;
import com.pet.requisition.application.handler.*;
import com.pet.requisition.application.model.*;
import com.pet.requisition.application.port.out.*;

public class MessageProcessingService {
    private final MessageCodecPort codec;
    private final PreparedRequestService preparedService;
    private final RequestHandlerExecutor executor;
    private final ResponseProcessingService responseService;

    public MessageProcessingService(MessageCodecPort codec, PreparedRequestService preparedService, RequestHandlerExecutor executor, ResponseProcessingService responseService) {
        this.codec = codec;
        this.preparedService = preparedService;
        this.executor = executor;
        this.responseService = responseService;
    }

    public String processMessage(String messageIncoming) throws Exception {
        RequestEnvelope<RawPayload> envelope = codec.deserializeRequest(messageIncoming);
        PreparedRequest<RawPayload> prepared;
        try {
            prepared = preparedService.prepare(envelope, messageIncoming);
        } catch (PreparationException e) {
            return responseService.error(e.getSystemParams(), e.getRequestRecord(), e.getStartedAt(), e.getCode(), e.getMessage());
        }
        try {
            Object result = executor.execute(prepared);
            return responseService.success(prepared, result);
        } catch (ProcessingException e) {
            return responseService.error(prepared.systemParams(), prepared.requestRecord(), prepared.startedAt(), e.getCode(), e.getMessage());
        } catch (Exception e) {
            return responseService.error(prepared.systemParams(), prepared.requestRecord(), prepared.startedAt(), "500", e.getMessage());
        }
    }
}
