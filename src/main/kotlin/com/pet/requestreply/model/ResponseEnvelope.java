package com.pet.requestreply.model;

public record ResponseEnvelope<T>(
        String correlationId,
        String requestType,
        T payload,
        ErrorInfo error
) {
    public static <T> ResponseEnvelope<T> success(String correlationId, String requestType, T payload) {
        return new ResponseEnvelope<>(correlationId, requestType, payload, null);
    }

    public static <T> ResponseEnvelope<T> error(String correlationId, String requestType, String code, String message) {
        return new ResponseEnvelope<>(correlationId, requestType, null, new ErrorInfo(code, message));
    }

    public boolean isSuccess() {
        return error == null;
    }
}
