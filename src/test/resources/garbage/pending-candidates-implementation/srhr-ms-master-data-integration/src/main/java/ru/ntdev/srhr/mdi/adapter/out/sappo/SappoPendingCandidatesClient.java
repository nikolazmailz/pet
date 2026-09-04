package ru.ntdev.srhr.mdi.adapter.out.sappo;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.ntdev.srhr.mdi.domain.PendingCandidatesIntegrationException;
import ru.ntdev.srhr.pending.contracts.PernrRequestEnvelope;

@Component
public class SappoPendingCandidatesClient {
    private final RestClient restClient;

    public SappoPendingCandidatesClient(RestClient sappoRestClient) {
        this.restClient = sappoRestClient;
    }

    public SappoPendingCandidatesResponse fetch(PernrRequestEnvelope request) {
        try {
            SappoPendingCandidatesResponse response = restClient.post()
                    .uri("/PendingCandidates")
                    .body(request)
                    .retrieve()
                    .body(SappoPendingCandidatesResponse.class);
            if (response == null || response.data() == null) {
                throw new PendingCandidatesIntegrationException("SAPPO вернул пустое тело ответа");
            }
            return response;
        } catch (PendingCandidatesIntegrationException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new PendingCandidatesIntegrationException("Не удалось получить кандидатов из SAPPO", ex);
        }
    }
}
