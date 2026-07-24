package ru.ntdev.srhr.pending.contracts;

import java.util.List;

public record PendingCandidateView(
        String candidateId,
        String vacancyId,
        String fullName,
        RequisitionView requisition,
        List<PendingCandidateEventView> events
) {
    public PendingCandidateView {
        events = events == null ? List.of() : List.copyOf(events);
    }
}
