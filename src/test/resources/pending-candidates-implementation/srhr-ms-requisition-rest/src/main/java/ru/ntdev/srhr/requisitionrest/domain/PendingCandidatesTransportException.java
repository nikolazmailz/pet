package ru.ntdev.srhr.requisitionrest.domain;

public class PendingCandidatesTransportException extends DomainException {
    public PendingCandidatesTransportException(String message, String traceId, Throwable cause) {
        super("PENDING_CANDIDATES_TRANSPORT_ERROR", message, traceId, cause);
    }
}
