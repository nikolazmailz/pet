package ru.ntdev.srhr.requisition.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ntdev.srhr.pending.contracts.PendingCandidateSnapshot;
import ru.ntdev.srhr.pending.contracts.PendingCandidatesPage;
import ru.ntdev.srhr.pending.contracts.PendingCandidatesPageRequest;

import java.util.List;

/**
 * Обновляет снимок и читает страницу в одной транзакции. Advisory lock остается
 * захваченным до завершения выборки, поэтому два параллельных запроса одного
 * согласующего не перемешивают удаление, вставку и чтение.
 */
@Service
public class ReplaceAndQueryPendingCandidates {
    private final RefreshPendingCandidatesSnapshot refreshSnapshot;
    private final PendingCandidatesQueryService queryService;

    public ReplaceAndQueryPendingCandidates(RefreshPendingCandidatesSnapshot refreshSnapshot,
                                            PendingCandidatesQueryService queryService) {
        this.refreshSnapshot = refreshSnapshot;
        this.queryService = queryService;
    }

    @Transactional
    public PendingCandidatesPage execute(String pernr,
                                         List<PendingCandidateSnapshot> candidates,
                                         PendingCandidatesPageRequest request) {
        refreshSnapshot.execute(pernr, candidates);
        return queryService.execute(pernr, request);
    }
}
