package ru.ntdev.srhr.mdi.adapter.out.sappo;

import java.util.List;

public record SappoPendingCandidatesResponse(Data data) {
    public record Data(List<Candidate> candidates) {}
    public record Candidate(String candidateId, String vacancyId, String fullName, List<Event> events) {}
    public record Event(String eventCode, Long statusDate, Integer days, Integer expirationZone) {}
}
