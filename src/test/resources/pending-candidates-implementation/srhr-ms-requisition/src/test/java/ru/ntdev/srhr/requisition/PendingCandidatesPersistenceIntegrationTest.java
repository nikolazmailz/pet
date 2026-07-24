package ru.ntdev.srhr.requisition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.ntdev.srhr.pending.contracts.PendingCandidateEventSnapshot;
import ru.ntdev.srhr.pending.contracts.PendingCandidateSnapshot;
import ru.ntdev.srhr.pending.contracts.PendingCandidatesFilter;
import ru.ntdev.srhr.pending.contracts.PendingCandidatesPageRequest;
import ru.ntdev.srhr.requisition.adapter.out.persistence.PendingCandidatesQueryRepository;
import ru.ntdev.srhr.requisition.application.RefreshPendingCandidatesSnapshot;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "integration.master-data.base-url=http://localhost:65534"
})
class PendingCandidatesPersistenceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("requisition")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired RefreshPendingCandidatesSnapshot refresh;
    @Autowired PendingCandidatesQueryRepository queryRepository;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.update("delete from pending_candidates");
    }

    @Test
    void filtersByEventWithOrSemanticsWithoutCandidateDuplicates() {
        refresh.execute("12345678", List.of(
                candidate("1", "101", "Иванов Иван",
                        event("rr_resume_review", "2026-01-01T00:00:00Z"),
                        event("rr_interview_3", "2026-01-02T00:00:00Z")),
                candidate("2", "102", "Петров Петр",
                        event("rr_offer", "2026-01-03T00:00:00Z")),
                candidate("3", "103", "Иванова Анна",
                        event("rr_interview_3", "2026-01-04T00:00:00Z"))
        ));

        var request = new PendingCandidatesPageRequest(1, 20,
                new PendingCandidatesFilter("Иван", List.of("rr_resume_review", "rr_interview_3")));

        var page = queryRepository.findPage("12345678", request);
        assertThat(page).extracting(row -> row.candidateId()).containsExactly("3", "1");
        assertThat(queryRepository.count("12345678", request)).isEqualTo(2);
        assertThat(queryRepository.findAvailableEventCodes("12345678", request))
                .containsExactly("rr_interview_3", "rr_resume_review");
    }

    @Test
    void replacesOnlySelectedApproverSnapshot() {
        refresh.execute("111", List.of(candidate("1", "101", "Первый", event("a", "2026-01-01T00:00:00Z"))));
        refresh.execute("222", List.of(candidate("2", "102", "Второй", event("b", "2026-01-01T00:00:00Z"))));
        refresh.execute("111", List.of());

        var request = new PendingCandidatesPageRequest(1, 20, PendingCandidatesFilter.empty());
        assertThat(queryRepository.count("111", request)).isZero();
        assertThat(queryRepository.count("222", request)).isEqualTo(1);
    }

    private PendingCandidateSnapshot candidate(String id, String vacancyId, String name,
                                                 PendingCandidateEventSnapshot... events) {
        return new PendingCandidateSnapshot(id, vacancyId, name, List.of(events));
    }

    private PendingCandidateEventSnapshot event(String code, String date) {
        return new PendingCandidateEventSnapshot(code, Instant.parse(date), 2, 1);
    }
}
