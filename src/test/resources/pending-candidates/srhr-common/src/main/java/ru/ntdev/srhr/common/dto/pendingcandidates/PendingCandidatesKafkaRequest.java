package ru.ntdev.srhr.common.dto.pendingcandidates;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Сообщение в топик srhr.requisition.to.easup.
 * Аннотации валидации продублированы: srhr-ms-requisition валидирует входящее
 * сообщение перед обработкой (defensive — источник сообщения не гарантирован).
 */
public record PendingCandidatesKafkaRequest(
        @NotNull @Min(1) Integer page,
        @NotNull @Min(1) @Max(100) Integer pageSize,
        @Valid PendingCandidatesFilter filter,
        @NotBlank String pernr) {

    public static PendingCandidatesKafkaRequest from(PendingCandidatesWebRequest web, String pernr) {
        return new PendingCandidatesKafkaRequest(web.page(), web.pageSize(), web.filter(), pernr);
    }
}
