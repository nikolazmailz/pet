package ru.ntdev.srhr.requisition.pendingcandidates.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ntdev.srhr.common.dto.pendingcandidates.EventCodeDto;
import ru.ntdev.srhr.common.dto.pendingcandidates.PendingCandidateDto;
import ru.ntdev.srhr.common.dto.pendingcandidates.PendingCandidateEventDto;
import ru.ntdev.srhr.common.dto.pendingcandidates.PendingCandidatesData;
import ru.ntdev.srhr.common.dto.pendingcandidates.PendingCandidatesFilter;
import ru.ntdev.srhr.common.dto.pendingcandidates.PendingCandidatesKafkaRequest;
import ru.ntdev.srhr.common.dto.pendingcandidates.PendingCandidatesResponse;
import ru.ntdev.srhr.common.dto.pendingcandidates.RequisitionShortDto;
import ru.ntdev.srhr.requisition.pendingcandidates.entity.PendingCandidate;
import ru.ntdev.srhr.requisition.pendingcandidates.entity.PendingCandidateEvent;
import ru.ntdev.srhr.requisition.pendingcandidates.repository.PendingCandidateRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Выборка кандидатов, ожидающих решения.
 *
 * <p>ИЗВЕСТНОЕ ОГРАНИЧЕНИЕ: каждый запрос полностью перезагружает снимок из
 * Е-стафф (см. {@link PendingCandidatesRefreshService}), поэтому пагинация
 * между запросами страниц неконсистентна по определению — Е-стафф может
 * отдать другой набор между страницей 1 и страницей 2. Принято как допустимое
 * поведение (решение №10 постановки).
 */
@Service
@RequiredArgsConstructor
public class PendingCandidatesQueryService {

    private final PendingCandidatesRefreshService refreshService;
    private final PendingCandidateRepository repository;
    private final RequisitionLookupPort requisitionLookupPort;

    public PendingCandidatesResponse find(PendingCandidatesKafkaRequest request) {
        refreshService.refresh(request.pernr());
        return query(request);
    }

    @Transactional(readOnly = true)
    protected PendingCandidatesResponse query(PendingCandidatesKafkaRequest request) {
        String pernr = request.pernr();
        String search = Optional.ofNullable(request.filter())
                .map(PendingCandidatesFilter::search)
                .filter(s -> !s.isBlank())
                .orElse(null);
        List<String> codes = Optional.ofNullable(request.filter())
                .map(PendingCandidatesFilter::eventCodeList)
                .filter(list -> !list.isEmpty())
                .orElse(null);

        long count = repository.countCandidates(pernr, search, codes);

        int offset = (request.page() - 1) * request.pageSize();
        List<Long> pageIds = repository.findPageIds(pernr, search, codes, request.pageSize(), offset);
        List<PendingCandidate> candidates = fetchInOrder(pageIds);

        List<EventCodeDto> facet = repository.findDistinctEventCodes(pernr, search).stream()
                .map(EventCodeDto::new)
                .toList();

        Set<String> vacancyIds = candidates.stream()
                .map(PendingCandidate::getVacancyId)
                .collect(Collectors.toSet());
        Map<String, RequisitionShortDto> requisitions =
                vacancyIds.isEmpty() ? Map.of() : requisitionLookupPort.findByVacancyIds(vacancyIds);

        List<PendingCandidateDto> candidateDtos = candidates.stream()
                .map(candidate -> toDto(candidate, requisitions.get(candidate.getVacancyId())))
                .toList();

        return PendingCandidatesResponse.ok(new PendingCandidatesData(
                request.page(), request.pageSize(), count, facet, candidateDtos));
    }

    /** IN-запрос не гарантирует порядок — восстанавливаем порядок pageIds. */
    private List<PendingCandidate> fetchInOrder(List<Long> pageIds) {
        if (pageIds.isEmpty()) {
            return List.of();
        }
        Map<Long, PendingCandidate> byId = repository.findWithEventsByIdIn(pageIds).stream()
                .collect(Collectors.toMap(PendingCandidate::getId, Function.identity()));
        return pageIds.stream().map(byId::get).toList();
    }

    private PendingCandidateDto toDto(PendingCandidate candidate, RequisitionShortDto requisition) {
        // @OrderBy на entity уже сортирует, повторная сортировка — защита
        // от изменения маппинга; statusDate наружу не отдаётся
        List<PendingCandidateEventDto> events = candidate.getEvents().stream()
                .sorted(Comparator.comparing(PendingCandidateEvent::getStatusDate))
                .map(e -> new PendingCandidateEventDto(e.getEventCode(), e.getDays(), e.getExpirationZone()))
                .toList();

        return new PendingCandidateDto(
                candidate.getCandidateId(),
                candidate.getVacancyId(),
                candidate.getFullName(),
                requisition,
                events);
    }
}
