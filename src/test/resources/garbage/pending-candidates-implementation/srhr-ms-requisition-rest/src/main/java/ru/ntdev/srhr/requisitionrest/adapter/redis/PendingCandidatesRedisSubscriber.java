package ru.ntdev.srhr.requisitionrest.adapter.redis;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import ru.ntdev.srhr.pending.contracts.PendingCandidatesKafkaResponse;
import ru.ntdev.srhr.requisitionrest.application.PendingResponseRegistry;

import java.nio.charset.StandardCharsets;

@Component
public class PendingCandidatesRedisSubscriber implements MessageListener {
    private final PendingCandidatesRedisResponseStore store;
    private final PendingResponseRegistry registry;

    public PendingCandidatesRedisSubscriber(PendingCandidatesRedisResponseStore store,
                                            PendingResponseRegistry registry) {
        this.store = store;
        this.registry = registry;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String correlationId = new String(message.getBody(), StandardCharsets.UTF_8);
        PendingCandidatesKafkaResponse response = store.get(correlationId);
        if (response != null) registry.complete(correlationId, response);
    }
}
