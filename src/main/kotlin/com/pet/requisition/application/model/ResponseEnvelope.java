package com.pet.requisition.application.model;
public record ResponseEnvelope<T>(
        String correlationId,
        String requestType,
        String sessionId,
        String adLogin,
        String traceId,
        String channel,
        String tabNumber,
        String rolesList,
        String fileUuid,
        T payload,
        ErrorInfo error
) {
    public static <T> ResponseEnvelope<T> success(SystemParams p, T payload) {
        return new ResponseEnvelope<>(p.getCorrelationId(),p.getRequestType(),p.getSessionId(),p.getAdLogin(),p.getTraceId(),p.getChannel(),p.getTabNumber(),p.getRolesHeader(),null,payload,null);
    }
    public static ResponseEnvelope<Void> error(SystemParams p, String code, String message) {
        return new ResponseEnvelope<>(p.getCorrelationId(),p.getRequestType(),p.getSessionId(),p.getAdLogin(),p.getTraceId(),p.getChannel(),p.getTabNumber(),p.getRolesHeader(),null,null,new ErrorInfo(code,message));
    }
    public ResponseEnvelope<Void> asStoredReference(String fileUuid) {
        return new ResponseEnvelope<>(correlationId,requestType,sessionId,adLogin,traceId,channel,tabNumber,rolesList,fileUuid,null,error);
    }
}
