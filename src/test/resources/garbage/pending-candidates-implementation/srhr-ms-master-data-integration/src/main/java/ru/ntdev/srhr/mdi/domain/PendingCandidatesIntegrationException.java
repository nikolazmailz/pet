package ru.ntdev.srhr.mdi.domain;

public class PendingCandidatesIntegrationException extends RuntimeException {
    public PendingCandidatesIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }

    public PendingCandidatesIntegrationException(String message) {
        super(message);
    }
}
