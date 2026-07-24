package ru.ntdev.srhr.requisitionrest.application;

import org.springframework.stereotype.Component;
import ru.ntdev.srhr.pending.contracts.PendingCandidatesKafkaResponse;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class PendingResponseRegistry {
    private final ConcurrentMap<String, CompletableFuture<PendingCandidatesKafkaResponse>> futures = new ConcurrentHashMap<>();

    public CompletableFuture<PendingCandidatesKafkaResponse> register(String correlationId) {
        CompletableFuture<PendingCandidatesKafkaResponse> future = new CompletableFuture<>();
        CompletableFuture<PendingCandidatesKafkaResponse> previous = futures.putIfAbsent(correlationId, future);
        if (previous != null) throw new IllegalStateException("Correlation id уже зарегистрирован: " + correlationId);
        return future;
    }

    public boolean complete(String correlationId, PendingCandidatesKafkaResponse response) {
        CompletableFuture<PendingCandidatesKafkaResponse> future = futures.remove(correlationId);
        return future != null && future.complete(response);
    }

    public void remove(String correlationId) {
        futures.remove(correlationId);
    }
}
