package ru.ntdev.srhr.common.dto.pendingcandidates;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Kafka-ответ из srhr.requisition.from.easup = REST-ответ фронту (один класс,
 * rest-сервис отдаёт тело как есть). Заполнено ровно одно из полей:
 * data при успехе, error при сбое.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PendingCandidatesResponse(PendingCandidatesData data, PendingCandidatesError error) {

    public static PendingCandidatesResponse ok(PendingCandidatesData data) {
        return new PendingCandidatesResponse(data, null);
    }

    public static PendingCandidatesResponse error(String code, String message) {
        return new PendingCandidatesResponse(null, new PendingCandidatesError(code, message));
    }
}
