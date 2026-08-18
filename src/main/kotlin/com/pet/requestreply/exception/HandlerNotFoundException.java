package com.pet.requestreply.exception;

public class HandlerNotFoundException extends RuntimeException {
    public HandlerNotFoundException(String requestType) {
        super("Handler not found for requestType: " + requestType);
    }
}
