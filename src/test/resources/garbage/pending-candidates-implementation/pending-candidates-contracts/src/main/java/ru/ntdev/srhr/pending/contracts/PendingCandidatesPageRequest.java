package ru.ntdev.srhr.pending.contracts;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PendingCandidatesPageRequest(
        @Min(1) int page,
        @Min(1) @Max(200) int pageSize,
        @Valid PendingCandidatesFilter filter
) {
    public PendingCandidatesPageRequest {
        filter = filter == null ? PendingCandidatesFilter.empty() : filter;
    }

    public int offset() {
        return Math.multiplyExact(page - 1, pageSize);
    }
}
