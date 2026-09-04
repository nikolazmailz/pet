package ru.ntdev.srhr.pending.contracts;

import java.util.List;

public record PendingCandidateSnapshot(
        String candidateId,
        String vacancyId,
        String fullName,
        List<PendingCandidateEventSnapshot> events
) {
    public PendingCandidateSnapshot {
        events = events == null ? List.of() : List.copyOf(events);
    }
}
