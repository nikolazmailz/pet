package ru.ntdev.srhr.pending.contracts;

public record PendingCandidatesKafkaResponse(
        String correlationId,
        PendingCandidatesPage data,
        KafkaError error
) {
    public static PendingCandidatesKafkaResponse success(String correlationId, PendingCandidatesPage data) {
        return new PendingCandidatesKafkaResponse(correlationId, data, null);
    }

    public static PendingCandidatesKafkaResponse failure(String correlationId, KafkaError error) {
        return new PendingCandidatesKafkaResponse(correlationId, null, error);
    }
}
