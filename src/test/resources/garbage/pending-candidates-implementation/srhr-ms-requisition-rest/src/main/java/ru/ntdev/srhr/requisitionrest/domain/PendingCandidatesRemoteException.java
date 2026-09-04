package ru.ntdev.srhr.requisitionrest.domain;

public class PendingCandidatesRemoteException extends DomainException {
    public PendingCandidatesRemoteException(String code, String message, String traceId) {
        super(code, message, traceId);
    }
}
