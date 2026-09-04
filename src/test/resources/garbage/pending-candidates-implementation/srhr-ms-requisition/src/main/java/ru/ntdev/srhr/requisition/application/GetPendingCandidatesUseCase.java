package ru.ntdev.srhr.requisition.application;

import org.springframework.stereotype.Service;
import ru.ntdev.srhr.pending.contracts.PendingCandidateSnapshot;
import ru.ntdev.srhr.pending.contracts.PendingCandidatesPage;
import ru.ntdev.srhr.pending.contracts.PendingCandidatesPageRequest;
import ru.ntdev.srhr.requisition.adapter.out.masterdata.MasterDataPendingCandidatesClient;

import java.util.List;

@Service
public class GetPendingCandidatesUseCase {
    private final MasterDataPendingCandidatesClient client;
    private final ReplaceAndQueryPendingCandidates replaceAndQuery;

    public GetPendingCandidatesUseCase(MasterDataPendingCandidatesClient client,
                                       ReplaceAndQueryPendingCandidates replaceAndQuery) {
        this.client = client;
        this.replaceAndQuery = replaceAndQuery;
    }

    public PendingCandidatesPage execute(String pernr, PendingCandidatesPageRequest request) {
        // Внешний вызов намеренно выполняется до открытия DB-транзакции.
        List<PendingCandidateSnapshot> actualCandidates = client.fetch(pernr);
        return replaceAndQuery.execute(pernr, actualCandidates, request);
    }
}
