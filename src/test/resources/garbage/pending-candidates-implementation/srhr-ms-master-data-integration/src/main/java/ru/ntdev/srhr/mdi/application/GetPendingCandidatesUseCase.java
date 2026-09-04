package ru.ntdev.srhr.mdi.application;

import org.springframework.stereotype.Service;
import ru.ntdev.srhr.mdi.adapter.out.sappo.SappoPendingCandidatesClient;
import ru.ntdev.srhr.pending.contracts.PendingCandidatesSnapshotResponse;
import ru.ntdev.srhr.pending.contracts.PernrRequestEnvelope;

@Service
public class GetPendingCandidatesUseCase {
    private final SappoPendingCandidatesClient client;
    private final PendingCandidatesMapper mapper;

    public GetPendingCandidatesUseCase(SappoPendingCandidatesClient client, PendingCandidatesMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    public PendingCandidatesSnapshotResponse execute(PernrRequestEnvelope request) {
        return mapper.toInternal(client.fetch(request));
    }
}
