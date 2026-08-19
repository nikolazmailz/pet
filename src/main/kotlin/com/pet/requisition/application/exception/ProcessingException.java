package com.pet.requisition.application.exception;

public class ProcessingException extends RuntimeException {
    private final String code;

    public ProcessingException(String code, String message) {
        super(message);
        this.code = code;
    }

    public ProcessingException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
