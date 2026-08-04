package ru.ntdev.srhr.common.dto.pendingcandidates.estaff;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EstaffPendingCandidatesResponse(EstaffPendingCandidatesData data) {
}
