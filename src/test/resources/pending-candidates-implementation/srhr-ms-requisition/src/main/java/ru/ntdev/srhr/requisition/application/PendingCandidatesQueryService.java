package ru.ntdev.srhr.requisition.application;

import org.springframework.stereotype.Service;
import ru.ntdev.srhr.pending.contracts.EventCodeView;
import ru.ntdev.srhr.pending.contracts.PendingCandidateEventView;
import ru.ntdev.srhr.pending.contracts.PendingCandidateView;
import ru.ntdev.srhr.pending.contracts.PendingCandidatesPage;
import ru.ntdev.srhr.pending.contracts.PendingCandidatesPageRequest;
import ru.ntdev.srhr.pending.contracts.RequisitionView;
import ru.ntdev.srhr.requisition.adapter.out.persistence.PendingCandidateEventRow;
import ru.ntdev.srhr.requisition.adapter.out.persistence.PendingCandidateRow;
import ru.ntdev.srhr.requisition.adapter.out.persistence.PendingCandidatesQueryRepository;
import ru.ntdev.srhr.requisition.adapter.out.persistence.RequisitionEnrichmentRepository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PendingCandidatesQueryService {
    private final PendingCandidatesQueryRepository repository;
    private final RequisitionEnrichmentRepository enrichmentRepository;

    public PendingCandidatesQueryService(PendingCandidatesQueryRepository repository,
                                         RequisitionEnrichmentRepository enrichmentRepository) {
        this.repository = repository;
        this.enrichmentRepository = enrichmentRepository;
    }

    public PendingCandidatesPage execute(String pernr, PendingCandidatesPageRequest request) {
        List<PendingCandidateRow> candidates = repository.findPage(pernr, request);
        long count = repository.count(pernr, request);
        List<EventCodeView> availableCodes = repository.findAvailableEventCodes(pernr, request).stream()
                .map(EventCodeView::new)
                .toList();

        List<Long> ids = candidates.stream().map(PendingCandidateRow::id).toList();
        Map<Long, List<PendingCandidateEventRow>> events = repository.findEvents(ids);
        Set<String> vacancyIds = candidates.stream()
                .map(PendingCandidateRow::vacancyId)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
        Map<String, RequisitionView> requisitions = enrichmentRepository.findByVacancyIds(vacancyIds);

        List<PendingCandidateView> views = candidates.stream().map(candidate -> new PendingCandidateView(
                candidate.candidateId(),
                candidate.vacancyId(),
                candidate.fullName(),
                requisitions.get(candidate.vacancyId()),
                events.getOrDefault(candidate.id(), List.of()).stream()
                        .map(event -> new PendingCandidateEventView(
                                event.eventCode(), event.days(), event.expirationZone()))
                        .toList()
        )).toList();

        return new PendingCandidatesPage(request.page(), request.pageSize(), count, availableCodes, views);
    }
}
