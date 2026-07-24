package ru.ntdev.srhr.common.dto.pendingcandidates.estaff;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @param statusDate epoch millis UTC; конвертация в Instant — задача маппера
 *                   на стороне srhr-ms-requisition, не DTO
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EstaffCandidateEvent(
        String eventCode,
        Long statusDate,
        Integer days,
        Integer expirationZone) {
}
