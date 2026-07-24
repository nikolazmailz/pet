package ru.ntdev.srhr.pending.contracts;

import java.util.List;

public record PendingCandidatesSnapshotData(List<PendingCandidateSnapshot> candidates) {
    public PendingCandidatesSnapshotData {
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
    }
}
