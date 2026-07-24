package ru.ntdev.srhr.requisitionrest.domain;

public abstract class DomainException extends RuntimeException {
    private final String code;
    private final String traceId;

    protected DomainException(String code, String message, String traceId) {
        super(message);
        this.code = code;
        this.traceId = traceId;
    }

    protected DomainException(String code, String message, String traceId, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.traceId = traceId;
    }

    public String getCode() { return code; }
    public String getTraceId() { return traceId; }
}
