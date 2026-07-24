package ru.ntdev.srhr.common.dto.pendingcandidates;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Тело запроса фронта {@code POST /pending-candidates}.
 * pernr с фронта НЕ принимается — rest-сервис извлекает его из JWT
 * и строит {@link PendingCandidatesKafkaRequest} сам.
 */
public record PendingCandidatesWebRequest(
        @NotNull @Min(1) Integer page,
        @NotNull @Min(1) @Max(100) Integer pageSize,
        @Valid PendingCandidatesFilter filter) {
}
