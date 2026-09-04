package ru.ntdev.srhr.requisition.pendingcandidates;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import ru.ntdev.srhr.common.dto.pendingcandidates.estaff.EstaffCandidate;
import ru.ntdev.srhr.common.dto.pendingcandidates.estaff.EstaffCandidateEvent;
import ru.ntdev.srhr.common.dto.pendingcandidates.estaff.EstaffPendingCandidatesData;
import ru.ntdev.srhr.common.dto.pendingcandidates.estaff.EstaffPendingCandidatesResponse;
import ru.ntdev.srhr.requisition.pendingcandidates.client.MasterDataIntegrationClient;
import ru.ntdev.srhr.requisition.pendingcandidates.client.PendingCandidatesFetchException;
import ru.ntdev.srhr.requisition.pendingcandidates.entity.PendingCandidate;
import ru.ntdev.srhr.requisition.pendingcandidates.repository.PendingCandidateRepository;
import ru.ntdev.srhr.requisition.pendingcandidates.service.PendingCandidatesRefreshService;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * ВАЖНО: без @Transactional на классе — refresh управляет транзакциями сам
 * через TransactionTemplate; тестовый rollback скрыл бы реальные границы.
 * Изоляция — через разные pernr в каждом тесте.
 */
@SpringBootTest
@DisplayName("PendingCandidatesRefreshService (Testcontainers)")
class PendingCandidatesRefreshServiceIT extends AbstractPostgresIT {

    @Autowired
    private PendingCandidatesRefreshService refreshService;

    @Autowired
    private PendingCandidateRepository repository;

    @MockBean
    private MasterDataIntegrationClient integrationClient;

    @Test
    @DisplayName("замена снимка: старые данные pernr удалены, новые сохранены с событиями")
    void replacesSnapshot() {
        String pernr = "10000001";
        when(integrationClient.fetchPendingCandidates(pernr))
                .thenReturn(response(candidate("c1", "v1", "Иванов", event("rr_interview_1", 1_701_000_000_000L))))
                .thenReturn(response(candidate("c2", "v2", "Петров", event("rr_interview_2", 1_702_000_000_000L))));

        refreshService.refresh(pernr);
        refreshService.refresh(pernr);

        List<PendingCandidate> stored = repository.findAll().stream()
                .filter(pc -> pernr.equals(pc.getApproverPernr()))
                .toList();
        assertThat(stored).singleElement().satisfies(pc -> {
            assertThat(pc.getCandidateId()).isEqualTo("c2");
            assertThat(pc.getEvents()).hasSize(1);
        });
    }

    @Test
    @DisplayName("конкурентные refresh одного pernr: advisory lock, ровно один итоговый снимок")
    void concurrentRefreshIsSerialized() throws Exception {
        String pernr = "10000002";
        when(integrationClient.fetchPendingCandidates(pernr)).thenAnswer(inv ->
                response(candidate("c1", "v1", "Иванов", event("rr_interview_1", 1_701_000_000_000L))));

        int threads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<?>> futures = IntStream.range(0, threads)
                    .mapToObj(i -> executor.submit(() -> {
                        start.await();
                        refreshService.refresh(pernr);
                        return null;
                    }))
                    .toList();
            start.countDown();
            for (Future<?> f : futures) {
                f.get(30, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        long count = repository.findAll().stream()
                .filter(pc -> pernr.equals(pc.getApproverPernr()))
                .count();
        assertThat(count).as("нет дублей и потерянных данных после гонки").isEqualTo(1);
    }

    @Test
    @DisplayName("ошибка Е-стафф: исключение пробрасывается, локальные данные не тронуты")
    void integrationFailureLeavesDataIntact() {
        String pernr = "10000003";
        when(integrationClient.fetchPendingCandidates(pernr))
                .thenReturn(response(candidate("c1", "v1", "Иванов", event("rr_interview_1", 1_701_000_000_000L))));
        refreshService.refresh(pernr);

        when(integrationClient.fetchPendingCandidates(anyString()))
                .thenThrow(new PendingCandidatesFetchException("boom"));

        assertThatThrownBy(() -> refreshService.refresh(pernr))
                .isInstanceOf(PendingCandidatesFetchException.class);

        long count = repository.findAll().stream()
                .filter(pc -> pernr.equals(pc.getApproverPernr()))
                .count();
        assertThat(count).isEqualTo(1);
    }

    private static EstaffPendingCandidatesResponse response(EstaffCandidate... candidates) {
        return new EstaffPendingCandidatesResponse(new EstaffPendingCandidatesData(List.of(candidates)));
    }

    private static EstaffCandidate candidate(String id, String vacancyId, String name,
                                             EstaffCandidateEvent... events) {
        return new EstaffCandidate(id, vacancyId, name, List.of(events));
    }

    private static EstaffCandidateEvent event(String code, long millis) {
        return new EstaffCandidateEvent(code, millis, 1, 1);
    }
}
