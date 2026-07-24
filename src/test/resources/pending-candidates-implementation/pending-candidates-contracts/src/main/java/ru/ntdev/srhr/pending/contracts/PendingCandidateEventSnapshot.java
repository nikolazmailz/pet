package ru.ntdev.srhr.pending.contracts;

import java.time.Instant;

public record PendingCandidateEventSnapshot(
        String eventCode,
        Instant statusDate,
        Integer days,
        Integer expirationZone
) {}
