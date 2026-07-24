package ru.ntdev.srhr.common.dto.pendingcandidates.estaff;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * ВАЖНО: SAPPO присылает candidateId/vacancyId числовыми JSON-токенами (19 знаков).
 * Поля объявлены как String — Jackson корректно десериализует и число, и строку;
 * дальше по системе идентификаторы всегда строки (защита от потери точности в JS).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EstaffCandidate(
        String candidateId,
        String vacancyId,
        String fullName,
        List<EstaffCandidateEvent> events) {
}
