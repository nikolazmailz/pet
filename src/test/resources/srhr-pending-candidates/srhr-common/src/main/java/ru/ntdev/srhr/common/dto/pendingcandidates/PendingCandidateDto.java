package ru.ntdev.srhr.common.dto.pendingcandidates;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * @param requisition null, если заявка по vacancyId не найдена — кандидат при этом
 *                    из выдачи НЕ исключается
 * @param events      отсортированы по status_date от раннего к позднему
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PendingCandidateDto(
        String candidateId,
        String vacancyId,
        String fullName,
        RequisitionShortDto requisition,
        List<PendingCandidateEventDto> events) {
}
