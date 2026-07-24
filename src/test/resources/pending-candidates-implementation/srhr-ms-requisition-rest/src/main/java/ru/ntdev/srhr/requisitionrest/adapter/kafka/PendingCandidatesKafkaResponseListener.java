package ru.ntdev.srhr.requisitionrest.adapter.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.ntdev.srhr.pending.contracts.PendingCandidatesKafkaResponse;
import ru.ntdev.srhr.requisitionrest.adapter.redis.PendingCandidatesRedisResponseStore;

@Component
public class PendingCandidatesKafkaResponseListener {
    private final PendingCandidatesRedisResponseStore store;

    public PendingCandidatesKafkaResponseListener(PendingCandidatesRedisResponseStore store) {
        this.store = store;
    }

    @KafkaListener(
            topics = "${pending-candidates.kafka.response-topic}",
            groupId = "${pending-candidates.kafka.response-group}"
    )
    public void onResponse(PendingCandidatesKafkaResponse response) {
        store.saveAndNotify(response);
    }
}
