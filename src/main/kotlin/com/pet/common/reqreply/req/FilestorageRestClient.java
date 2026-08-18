package com.pet.common.reqreply.req;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * HTTP-клиент filestorage. Создаётся один раз (бином в конфиге сервиса),
 * RestTemplate переиспользуется; per-request заголовки передаются через HttpEntity.
 */
public class FilestorageRestClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public FilestorageRestClient(ObjectMapper objectMapper, String baseUrl) {
        this(objectMapper, baseUrl, List.of());
    }

    public FilestorageRestClient(ObjectMapper objectMapper,
                                 String baseUrl,
                                 List<ClientHttpRequestInterceptor> interceptors) {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper);

        this.restTemplate = new RestTemplateBuilder()
                .messageConverters(converter)
                .additionalInterceptors(interceptors)
                .build();
        this.baseUrl = baseUrl;
    }

    public <REQ, RESP> RESP post(String path, REQ body, Class<RESP> responseType,
                                 String adLogin, String sessionId, String traceId) {
        HttpEntity<REQ> entity = new HttpEntity<>(body, headers(adLogin, sessionId, traceId));
        ResponseEntity<RESP> response = restTemplate.postForEntity(baseUrl + path, entity, responseType);
        return response.getBody();
    }

    public <RESP> RESP get(String path, Class<RESP> responseType,
                           String adLogin, String sessionId, String traceId) {
        HttpEntity<Void> entity = new HttpEntity<>(headers(adLogin, sessionId, traceId));
        ResponseEntity<RESP> response = restTemplate.exchange(
                baseUrl + path, org.springframework.http.HttpMethod.GET, entity, responseType);
        return response.getBody();
    }

    private HttpHeaders headers(String adLogin, String sessionId, String traceId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8));
        headers.set("pernr", adLogin);
        headers.set("sessionId", sessionId);
        headers.set("logId", traceId);
        return headers;
    }
}
