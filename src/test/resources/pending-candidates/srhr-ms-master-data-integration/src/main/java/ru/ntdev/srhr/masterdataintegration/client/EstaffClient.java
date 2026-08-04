package ru.ntdev.srhr.masterdataintegration.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import ru.ntdev.srhr.common.dto.pendingcandidates.estaff.EstaffPendingCandidatesRequest;
import ru.ntdev.srhr.common.dto.pendingcandidates.estaff.EstaffPendingCandidatesResponse;
import ru.ntdev.srhr.masterdataintegration.config.EstaffProperties;
import ru.ntdev.srhr.masterdataintegration.exception.EstaffIntegrationException;

/**
 * Клиент SAPPO (Е-стафф). Использует общий singleton integrationRestTemplate
 * (пул соединений, таймауты — в его конфигурации, здесь не дублируются).
 */
@Component
@RequiredArgsConstructor
public class EstaffClient {

    private final RestTemplate integrationRestTemplate;
    private final EstaffProperties properties;

    public EstaffPendingCandidatesResponse fetchPendingCandidates(String pernr) {
        String url = properties.baseUrl() + "/PendingCandidates";
        try {
            EstaffPendingCandidatesResponse response = integrationRestTemplate.postForObject(
                    url, EstaffPendingCandidatesRequest.of(pernr), EstaffPendingCandidatesResponse.class);
            if (response == null) {
                throw new EstaffIntegrationException("Е-стафф вернул пустое тело ответа");
            }
            return response;
        } catch (RestClientException e) {
            throw new EstaffIntegrationException("Ошибка запроса к Е-стафф: " + e.getMessage(), e);
        }
    }
}
