package ru.ntdev.srhr.requisitionrest.domain;

public class PendingCandidatesTimeoutException extends DomainException {
    public PendingCandidatesTimeoutException(String traceId, Throwable cause) {
        super("PENDING_CANDIDATES_TIMEOUT", "Превышено время ожидания списка кандидатов", traceId, cause);
    }
}
