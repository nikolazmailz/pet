package ru.ntdev.srhr.requisitionrest.pendingcandidates;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.ntdev.srhr.common.dto.pendingcandidates.PendingCandidatesKafkaRequest;
import ru.ntdev.srhr.common.dto.pendingcandidates.PendingCandidatesResponse;
import ru.ntdev.srhr.common.dto.pendingcandidates.PendingCandidatesWebRequest;
import ru.ntdev.srhr.reqreply.KafkaRedisRequestReplyTemplate;
import ru.ntdev.srhr.reqreply.ReplyTimeoutException;

import static ru.ntdev.srhr.common.dto.pendingcandidates.PendingCandidatesError.INTERNAL_ERROR;

/**
 * Фасад для фронта. Тело Kafka-ответа отдаётся как есть (общий контракт
 * PendingCandidatesResponse). Транспорт — srhr-reqreply-starter
 * (Kafka + Redis, correlation id).
 *
 * Маппинг ошибок: error в ответе -> 502, таймаут req-reply -> 504.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class PendingCandidatesRestController {

    private final KafkaRedisRequestReplyTemplate<PendingCandidatesKafkaRequest, PendingCandidatesResponse>
            requestReplyTemplate;
    private final CurrentUserPernrResolver pernrResolver;
    private final PendingCandidatesRestProperties properties;

    @PostMapping("/garbage/pending-candidates")
    public ResponseEntity<PendingCandidatesResponse> pendingCandidates(
            @Valid @RequestBody PendingCandidatesWebRequest webRequest,
            Authentication authentication) {

        String pernr = pernrResolver.resolve(authentication);
        PendingCandidatesKafkaRequest kafkaRequest =
                PendingCandidatesKafkaRequest.from(webRequest, pernr);

        PendingCandidatesResponse response;
        try {
            response = requestReplyTemplate.sendAndReceive(
                    properties.requestTopic(),
                    kafkaRequest,
                    PendingCandidatesResponse.class,
                    properties.replyTimeout());
        } catch (ReplyTimeoutException e) {
            log.error("Таймаут ожидания ответа pending-candidates, pernr={}", pernr, e);
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                    .body(PendingCandidatesResponse.error(INTERNAL_ERROR,
                            "Превышено время ожидания ответа"));
        }

        if (response.error() != null) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
        }
        return ResponseEntity.ok(response);
    }
}
