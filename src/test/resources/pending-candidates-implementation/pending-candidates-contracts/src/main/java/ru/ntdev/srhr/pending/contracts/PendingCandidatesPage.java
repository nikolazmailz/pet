package ru.ntdev.srhr.pending.contracts;

import java.util.List;

public record PendingCandidatesPage(
        int page,
        int pageSize,
        long count,
        List<EventCodeView> eventCode,
        List<PendingCandidateView> candidates
) {
    public PendingCandidatesPage {
        eventCode = eventCode == null ? List.of() : List.copyOf(eventCode);
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
    }
}
