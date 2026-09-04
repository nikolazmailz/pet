package ru.ntdev.srhr.requisitionrest;

import org.junit.jupiter.api.Test;
import ru.ntdev.srhr.pending.contracts.PendingCandidatesKafkaResponse;
import ru.ntdev.srhr.pending.contracts.PendingCandidatesPage;
import ru.ntdev.srhr.requisitionrest.application.PendingResponseRegistry;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PendingResponseRegistryTest {
    @Test
    void completesRegisteredFutureOnce() {
        PendingResponseRegistry registry = new PendingResponseRegistry();
        var future = registry.register("id-1");
        var response = PendingCandidatesKafkaResponse.success("id-1",
                new PendingCandidatesPage(1, 20, 0, List.of(), List.of()));

        assertThat(registry.complete("id-1", response)).isTrue();
        assertThat(registry.complete("id-1", response)).isFalse();
        assertThat(future.join()).isEqualTo(response);
    }
}
