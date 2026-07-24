package ru.ntdev.srhr.requisition.domain;

public class PendingCandidatesIntegrationException extends DomainException {
    public PendingCandidatesIntegrationException(String message, Throwable cause) {
        super("PENDING_CANDIDATES_INTEGRATION_ERROR", message, cause);
    }
}
