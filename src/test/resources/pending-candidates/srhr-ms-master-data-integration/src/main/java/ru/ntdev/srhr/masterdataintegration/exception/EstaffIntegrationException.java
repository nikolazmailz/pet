package ru.ntdev.srhr.masterdataintegration.exception;

public class EstaffIntegrationException extends RuntimeException {

    public EstaffIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }

    public EstaffIntegrationException(String message) {
        super(message);
    }
}
