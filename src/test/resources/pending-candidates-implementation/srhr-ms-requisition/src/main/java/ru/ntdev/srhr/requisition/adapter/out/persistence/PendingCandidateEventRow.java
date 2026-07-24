package ru.ntdev.srhr.requisition.adapter.out.persistence;

import java.time.Instant;

public record PendingCandidateEventRow(long id, long pendingCandidateId, String eventCode, Instant statusDate, Integer days, Integer expirationZone) {}
