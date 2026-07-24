package ru.ntdev.srhr.pending.contracts;

public record PendingCandidatesKafkaRequest(
        String correlationId,
        String traceId,
        String pernr,
        PendingCandidatesPageRequest request
) {}
