package ru.ntdev.srhr.requisition.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ntdev.srhr.pending.contracts.PendingCandidateEventSnapshot;
import ru.ntdev.srhr.pending.contracts.PendingCandidateSnapshot;
import ru.ntdev.srhr.requisition.adapter.out.persistence.PendingCandidatesSnapshotRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RefreshPendingCandidatesSnapshot {
    private final PendingCandidatesSnapshotRepository repository;

    public RefreshPendingCandidatesSnapshot(PendingCandidatesSnapshotRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void execute(String pernr, List<PendingCandidateSnapshot> source) {
        repository.lockApprover(pernr);
        repository.deleteByApprover(pernr);
        for (PendingCandidateSnapshot candidate : normalize(source)) {
            long id = repository.insertCandidate(pernr, candidate);
            for (PendingCandidateEventSnapshot event : candidate.events()) {
                repository.insertEvent(id, event);
            }
        }
    }

    private List<PendingCandidateSnapshot> normalize(List<PendingCandidateSnapshot> source) {
        if (source == null || source.isEmpty()) return List.of();
        Map<String, MutableCandidate> grouped = new LinkedHashMap<>();
        for (PendingCandidateSnapshot candidate : source) {
            if (candidate == null) continue;
            String key = candidate.candidateId() + "\u0000" + candidate.vacancyId();
            MutableCandidate value = grouped.computeIfAbsent(key,
                    ignored -> new MutableCandidate(candidate.candidateId(), candidate.vacancyId(), candidate.fullName()));
            value.events.addAll(candidate.events());
        }
        return grouped.values().stream()
                .map(value -> new PendingCandidateSnapshot(value.candidateId, value.vacancyId, value.fullName, value.events))
                .toList();
    }

    private static final class MutableCandidate {
        private final String candidateId;
        private final String vacancyId;
        private final String fullName;
        private final List<PendingCandidateEventSnapshot> events = new ArrayList<>();

        private MutableCandidate(String candidateId, String vacancyId, String fullName) {
            this.candidateId = candidateId;
            this.vacancyId = vacancyId;
            this.fullName = fullName;
        }
    }
}
