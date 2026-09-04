package ru.ntdev.srhr.requisition.pendingcandidates.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import ru.ntdev.srhr.common.dto.pendingcandidates.estaff.EstaffPendingCandidatesRequest;
import ru.ntdev.srhr.common.dto.pendingcandidates.estaff.EstaffPendingCandidatesResponse;
import ru.ntdev.srhr.requisition.pendingcandidates.config.PendingCandidatesProperties;

/**
 * Клиент srhr-ms-master-data-integration (прокси к Е-стафф).
 * Использует общий singleton integrationRestTemplate.
 */
@Component
@RequiredArgsConstructor
public class MasterDataIntegrationClient {

    private final RestTemplate integrationRestTemplate;
    private final PendingCandidatesProperties properties;

    public EstaffPendingCandidatesResponse fetchPendingCandidates(String pernr) {
        String url = properties.masterDataIntegrationBaseUrl() + "/garbage/pending-candidates";
        try {
            EstaffPendingCandidatesResponse response = integrationRestTemplate.postForObject(
                    url, EstaffPendingCandidatesRequest.of(pernr), EstaffPendingCandidatesResponse.class);
            if (response == null) {
                throw new PendingCandidatesFetchException(
                        "master-data-integration вернул пустое тело ответа");
            }
            return response;
        } catch (RestClientException e) {
            throw new PendingCandidatesFetchException(
                    "Не удалось получить кандидатов из Е-стафф: " + e.getMessage(), e);
        }
    }
}
