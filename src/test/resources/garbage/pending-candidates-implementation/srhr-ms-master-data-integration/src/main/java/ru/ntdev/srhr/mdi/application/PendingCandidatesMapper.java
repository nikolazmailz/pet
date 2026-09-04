package ru.ntdev.srhr.mdi.application;

import org.springframework.stereotype.Component;
import ru.ntdev.srhr.mdi.adapter.out.sappo.SappoPendingCandidatesResponse;
import ru.ntdev.srhr.pending.contracts.PendingCandidateEventSnapshot;
import ru.ntdev.srhr.pending.contracts.PendingCandidateSnapshot;
import ru.ntdev.srhr.pending.contracts.PendingCandidatesSnapshotData;
import ru.ntdev.srhr.pending.contracts.PendingCandidatesSnapshotResponse;

import java.time.Instant;
import java.util.List;

@Component
public class PendingCandidatesMapper {
    public PendingCandidatesSnapshotResponse toInternal(SappoPendingCandidatesResponse source) {
        List<PendingCandidateSnapshot> candidates = source.data().candidates() == null
                ? List.of()
                : source.data().candidates().stream().map(this::mapCandidate).toList();
        return new PendingCandidatesSnapshotResponse(new PendingCandidatesSnapshotData(candidates));
    }

    private PendingCandidateSnapshot mapCandidate(SappoPendingCandidatesResponse.Candidate source) {
        List<PendingCandidateEventSnapshot> events = source.events() == null
                ? List.of()
                : source.events().stream().map(this::mapEvent).toList();
        return new PendingCandidateSnapshot(
                source.candidateId(),
                source.vacancyId(),
                source.fullName(),
                events
        );
    }

    private PendingCandidateEventSnapshot mapEvent(SappoPendingCandidatesResponse.Event source) {
        Instant statusDate = source.statusDate() == null ? null : Instant.ofEpochMilli(source.statusDate());
        return new PendingCandidateEventSnapshot(
                source.eventCode(),
                statusDate,
                source.days(),
                source.expirationZone()
        );
    }
}
