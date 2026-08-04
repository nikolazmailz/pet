package ru.ntdev.srhr.common.dto.pendingcandidates;

public record PendingCandidatesError(String code, String message) {

    public static final String ESTAFF_UNAVAILABLE = "ESTAFF_UNAVAILABLE";
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
}
