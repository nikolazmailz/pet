package ru.ntdev.srhr.requisition.pendingcandidates.kafka;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import ru.ntdev.srhr.common.dto.pendingcandidates.PendingCandidatesKafkaRequest;
import ru.ntdev.srhr.common.dto.pendingcandidates.PendingCandidatesResponse;
import ru.ntdev.srhr.requisition.pendingcandidates.client.PendingCandidatesFetchException;
import ru.ntdev.srhr.requisition.pendingcandidates.config.PendingCandidatesProperties;
import ru.ntdev.srhr.requisition.pendingcandidates.service.PendingCandidatesQueryService;

import java.util.Set;
import java.util.stream.Collectors;

import static ru.ntdev.srhr.common.dto.pendingcandidates.PendingCandidatesError.ESTAFF_UNAVAILABLE;
import static ru.ntdev.srhr.common.dto.pendingcandidates.PendingCandidatesError.INTERNAL_ERROR;
import static ru.ntdev.srhr.common.dto.pendingcandidates.PendingCandidatesError.VALIDATION_ERROR;

/**
 * Request-reply: запрос из srhr.requisition.to.easup, ответ в
 * srhr.requisition.from.easup с прокинутым correlation id.
 * Любой исход (включая ошибку) обязан породить ответ — иначе rest-сервис
 * будет ждать до таймаута.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PendingCandidatesKafkaListener {

    private final PendingCandidatesQueryService queryService;
    private final KafkaTemplate<String, PendingCandidatesResponse> kafkaTemplate;
    private final Validator validator;
    private final PendingCandidatesProperties properties;

    @KafkaListener(
            topics = "${srhr.pending-candidates.request-topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "pendingCandidatesListenerContainerFactory")
    public void onRequest(@Payload PendingCandidatesKafkaRequest request,
                          @Header(KafkaHeaders.CORRELATION_ID) byte[] correlationId) {
        PendingCandidatesResponse response = handle(request);

        ProducerRecord<String, PendingCandidatesResponse> reply =
                new ProducerRecord<>(properties.replyTopic(), response);
        reply.headers().add(KafkaHeaders.CORRELATION_ID, correlationId);
        kafkaTemplate.send(reply);
    }

    private PendingCandidatesResponse handle(PendingCandidatesKafkaRequest request) {
        Set<ConstraintViolation<PendingCandidatesKafkaRequest>> violations =
                validator.validate(request);
        if (!violations.isEmpty()) {
            String details = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining("; "));
            log.warn("Невалидный запрос pending-candidates: {}", details);
            return PendingCandidatesResponse.error(VALIDATION_ERROR, details);
        }

        try {
            return queryService.find(request);
        } catch (PendingCandidatesFetchException e) {
            log.error("Е-стафф недоступен, pernr={}", request.pernr(), e);
            return PendingCandidatesResponse.error(ESTAFF_UNAVAILABLE, e.getMessage());
        } catch (Exception e) {
            log.error("Ошибка обработки pending-candidates, pernr={}", request.pernr(), e);
            return PendingCandidatesResponse.error(INTERNAL_ERROR,
                    "Внутренняя ошибка обработки запроса");
        }
    }
}
