package ru.ntdev.srhr.requisition.pendingcandidates.client;

public class PendingCandidatesFetchException extends RuntimeException {

    public PendingCandidatesFetchException(String message, Throwable cause) {
        super(message, cause);
    }

    public PendingCandidatesFetchException(String message) {
        super(message);
    }
}
