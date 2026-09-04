package ru.ntdev.srhr.requisition.adapter.in.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.ntdev.srhr.pending.contracts.KafkaError;
import ru.ntdev.srhr.pending.contracts.PendingCandidatesKafkaRequest;
import ru.ntdev.srhr.pending.contracts.PendingCandidatesKafkaResponse;
import ru.ntdev.srhr.pending.contracts.PendingCandidatesPage;
import ru.ntdev.srhr.requisition.application.GetPendingCandidatesUseCase;
import ru.ntdev.srhr.requisition.domain.DomainException;

@Component
public class PendingCandidatesKafkaListener {
    private static final Logger log = LoggerFactory.getLogger(PendingCandidatesKafkaListener.class);

    private final GetPendingCandidatesUseCase useCase;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String responseTopic;

    public PendingCandidatesKafkaListener(GetPendingCandidatesUseCase useCase,
                                           KafkaTemplate<String, Object> kafkaTemplate,
                                           @Value("${pending-candidates.kafka.response-topic}") String responseTopic) {
        this.useCase = useCase;
        this.kafkaTemplate = kafkaTemplate;
        this.responseTopic = responseTopic;
    }

    @KafkaListener(
            topics = "${pending-candidates.kafka.request-topic}",
            groupId = "${pending-candidates.kafka.request-group}"
    )
    public void onRequest(PendingCandidatesKafkaRequest request) {
        PendingCandidatesKafkaResponse response;
        try {
            PendingCandidatesPage data = useCase.execute(request.pernr(), request.request());
            response = PendingCandidatesKafkaResponse.success(request.correlationId(), data);
        } catch (DomainException ex) {
            log.warn("Pending candidates request failed, uuid={}, code={}",
                    request.correlationId(), ex.getCode(), ex);
            response = PendingCandidatesKafkaResponse.failure(request.correlationId(),
                    new KafkaError(ex.getCode(), ex.getMessage(), request.traceId()));
        } catch (Exception ex) {
            log.error("Unexpected pending candidates error, uuid={}", request.correlationId(), ex);
            response = PendingCandidatesKafkaResponse.failure(request.correlationId(),
                    new KafkaError("PENDING_CANDIDATES_UNEXPECTED_ERROR",
                            "Непредвиденная ошибка получения кандидатов", request.traceId()));
        }
        kafkaTemplate.send(responseTopic, request.correlationId(), response);
    }
}
