package ru.ntdev.srhr.common.dto.pendingcandidates.estaff;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EstaffPendingCandidatesData(List<EstaffCandidate> candidates) {
}
