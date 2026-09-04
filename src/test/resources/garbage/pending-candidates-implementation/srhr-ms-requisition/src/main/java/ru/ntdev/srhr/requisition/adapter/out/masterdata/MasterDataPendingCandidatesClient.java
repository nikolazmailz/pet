package ru.ntdev.srhr.requisition.adapter.out.masterdata;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.ntdev.srhr.pending.contracts.PendingCandidateSnapshot;
import ru.ntdev.srhr.pending.contracts.PendingCandidatesSnapshotResponse;
import ru.ntdev.srhr.pending.contracts.PernrRequest;
import ru.ntdev.srhr.pending.contracts.PernrRequestEnvelope;
import ru.ntdev.srhr.requisition.domain.PendingCandidatesIntegrationException;

import java.util.List;

@Component
public class MasterDataPendingCandidatesClient {
    private final RestClient restClient;

    public MasterDataPendingCandidatesClient(RestClient masterDataRestClient) {
        this.restClient = masterDataRestClient;
    }

    public List<PendingCandidateSnapshot> fetch(String pernr) {
        try {
            PendingCandidatesSnapshotResponse response = restClient.post()
                    .uri("/garbage/pending-candidates")
                    .body(new PernrRequestEnvelope(new PernrRequest(pernr)))
                    .retrieve()
                    .body(PendingCandidatesSnapshotResponse.class);
            if (response == null || response.data() == null) {
                throw new PendingCandidatesIntegrationException("Master-data-integration вернул пустой ответ", null);
            }
            return response.data().candidates();
        } catch (PendingCandidatesIntegrationException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new PendingCandidatesIntegrationException("Ошибка вызова master-data-integration", ex);
        }
    }
}
