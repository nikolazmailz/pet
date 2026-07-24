package ru.ntdev.srhr.common.dto.pendingcandidates;

/**
 * Наружу поле называется {@code code} (во внутренних контрактах — eventCode).
 * statusDate фронту не отдаётся: используется только для серверной сортировки.
 */
public record PendingCandidateEventDto(String code, Integer days, Integer expirationZone) {
}
