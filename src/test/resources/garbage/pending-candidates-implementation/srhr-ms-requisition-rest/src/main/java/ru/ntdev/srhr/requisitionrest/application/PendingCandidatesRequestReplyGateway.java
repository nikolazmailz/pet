package ru.ntdev.srhr.requisitionrest.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.ntdev.srhr.pending.contracts.PendingCandidatesKafkaRequest;
import ru.ntdev.srhr.pending.contracts.PendingCandidatesKafkaResponse;
import ru.ntdev.srhr.pending.contracts.PendingCandidatesPage;
import ru.ntdev.srhr.pending.contracts.PendingCandidatesPageRequest;
import ru.ntdev.srhr.requisitionrest.config.PendingCandidatesRequestReplyProperties;
import ru.ntdev.srhr.requisitionrest.domain.PendingCandidatesRemoteException;
import ru.ntdev.srhr.requisitionrest.domain.PendingCandidatesTimeoutException;
import ru.ntdev.srhr.requisitionrest.domain.PendingCandidatesTransportException;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class PendingCandidatesRequestReplyGateway {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final PendingResponseRegistry registry;
    private final PendingCandidatesRequestReplyProperties properties;
    private final String requestTopic;

    public PendingCandidatesRequestReplyGateway(KafkaTemplate<String, Object> kafkaTemplate,
                                                PendingResponseRegistry registry,
                                                PendingCandidatesRequestReplyProperties properties,
                                                @Value("${pending-candidates.kafka.request-topic}") String requestTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.registry = registry;
        this.properties = properties;
        this.requestTopic = requestTopic;
    }

    public PendingCandidatesPage execute(String pernr, String traceId, PendingCandidatesPageRequest pageRequest) {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<PendingCandidatesKafkaResponse> future = registry.register(correlationId);
        try {
            PendingCandidatesKafkaRequest request = new PendingCandidatesKafkaRequest(
                    correlationId, traceId, pernr, pageRequest);
            kafkaTemplate.send(requestTopic, correlationId, request)
                    .get(Math.min(5_000, properties.timeout().toMillis()), TimeUnit.MILLISECONDS);

            PendingCandidatesKafkaResponse response = future.get(properties.timeout().toMillis(), TimeUnit.MILLISECONDS);
            if (response.error() != null) {
                throw new PendingCandidatesRemoteException(
                        response.error().code(), response.error().message(), response.error().traceId());
            }
            return response.data();
        } catch (TimeoutException ex) {
            throw new PendingCandidatesTimeoutException(traceId, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new PendingCandidatesTransportException("Ожидание ответа было прервано", traceId, ex);
        } catch (ExecutionException ex) {
            throw new PendingCandidatesTransportException("Не удалось отправить или получить сообщение", traceId, ex.getCause());
        } finally {
            registry.remove(correlationId);
        }
    }
}
