package ru.ntdev.srhr.requisition.domain;

public class PendingCandidatesPersistenceException extends DomainException {
    public PendingCandidatesPersistenceException(String message, Throwable cause) {
        super("PENDING_CANDIDATES_PERSISTENCE_ERROR", message, cause);
    }
}
