package ru.ntdev.srhr.requisition.pendingcandidates;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import ru.ntdev.srhr.common.dto.pendingcandidates.EventCodeDto;
import ru.ntdev.srhr.common.dto.pendingcandidates.PendingCandidateDto;
import ru.ntdev.srhr.common.dto.pendingcandidates.PendingCandidatesFilter;
import ru.ntdev.srhr.common.dto.pendingcandidates.PendingCandidatesKafkaRequest;
import ru.ntdev.srhr.common.dto.pendingcandidates.PendingCandidatesResponse;
import ru.ntdev.srhr.common.dto.pendingcandidates.RequisitionShortDto;
import ru.ntdev.srhr.requisition.pendingcandidates.entity.PendingCandidate;
import ru.ntdev.srhr.requisition.pendingcandidates.entity.PendingCandidateEvent;
import ru.ntdev.srhr.requisition.pendingcandidates.repository.PendingCandidateRepository;
import ru.ntdev.srhr.requisition.pendingcandidates.service.PendingCandidatesQueryService;
import ru.ntdev.srhr.requisition.pendingcandidates.service.PendingCandidatesRefreshService;
import ru.ntdev.srhr.requisition.pendingcandidates.service.RequisitionLookupPort;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * refresh замокан (интеграция покрыта PendingCandidatesRefreshServiceIT) —
 * тестовые данные сажаются напрямую в таблицы, проверяется чистая выборка.
 */
@SpringBootTest
@DisplayName("PendingCandidatesQueryService (Testcontainers)")
class PendingCandidatesQueryServiceIT extends AbstractPostgresIT {

    private static final String PERNR = "20000001";
    private static final String OTHER_PERNR = "20000099";

    @Autowired
    private PendingCandidatesQueryService queryService;

    @Autowired
    private PendingCandidateRepository repository;

    @MockBean
    private PendingCandidatesRefreshService refreshService;

    @MockBean
    private RequisitionLookupPort requisitionLookupPort;

    @BeforeEach
    void setUp() {
        doNothing().when(refreshService).refresh(anyString());
        when(requisitionLookupPort.findByVacancyIds(anySet())).thenReturn(Map.of());
        repository.deleteAll();

        // Ближайшие дедлайны: Сидоров(t1) < Иванов(t2) < Петров-Водкин(t3)
        save(candidate("c-sidorov", "v1", "Сидоров Пётр Петрович",
                event("rr_resume_review", t(1))));
        save(candidate("c-ivanov", "v2", "Иванов Иван Иванович",
                event("rr_interview_3", t(5)), event("rr_offer", t(2))));
        save(candidate("c-petrov", "v3", "Петров-Водкин Кузьма Сергеевич",
                event("rr_interview_3", t(3))));
        // Шум другого руководителя — не должен попадать в выборку
        save(other("c-noise", "v9", "Иванов Клон Иванович", event("rr_secret", t(1))));
    }

    @Nested
    @DisplayName("Фильтрация и поиск")
    class Filtering {

        @Test
        @DisplayName("без фильтров: только кандидаты своего pernr, count = 3")
        void noFilters() {
            var data = find(request(1, 10, null, null)).data();

            assertThat(data.count()).isEqualTo(3);
            assertThat(names(data.candidates()))
                    .doesNotContain("Иванов Клон Иванович");
        }

        @Test
        @DisplayName("фильтр по кодам — условие ИЛИ")
        void eventCodesOrSemantics() {
            var data = find(request(1, 10, null,
                    List.of("rr_offer", "rr_resume_review"))).data();

            assertThat(data.count()).isEqualTo(2);
            assertThat(names(data.candidates()))
                    .containsExactlyInAnyOrder("Сидоров Пётр Петрович", "Иванов Иван Иванович");
        }

        @Test
        @DisplayName("поиск по ФИО: полное вхождение, регистронезависимо")
        void searchByFullName() {
            var data = find(request(1, 10, "иванов", null)).data();

            assertThat(data.count()).isEqualTo(1);
            assertThat(names(data.candidates())).containsExactly("Иванов Иван Иванович");
        }

        @Test
        @DisplayName("спецсимволы LIKE экранируются: '%' — литерал, не wildcard")
        void likeSpecialCharsEscaped() {
            var data = find(request(1, 10, "%", null)).data();

            assertThat(data.count()).isZero();
            assertThat(data.candidates()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Сортировка и пагинация")
    class SortingAndPaging {

        @Test
        @DisplayName("кандидаты отсортированы по ближайшему дедлайну (MIN(status_date))")
        void sortedByNearestDeadline() {
            var data = find(request(1, 10, null, null)).data();

            // Иванов: MIN(t5, t2) = t2 -> вторым, несмотря на позднее t5
            assertThat(names(data.candidates())).containsExactly(
                    "Сидоров Пётр Петрович",
                    "Иванов Иван Иванович",
                    "Петров-Водкин Кузьма Сергеевич");
        }

        @Test
        @DisplayName("пагинация: страница 2 при pageSize=2, count не зависит от страницы")
        void secondPage() {
            var data = find(request(2, 2, null, null)).data();

            assertThat(data.count()).isEqualTo(3);
            assertThat(names(data.candidates()))
                    .containsExactly("Петров-Водкин Кузьма Сергеевич");
        }

        @Test
        @DisplayName("events кандидата отсортированы по status_date от раннего к позднему")
        void eventsSortedAscending() {
            var data = find(request(1, 10, "Иванов Иван", null)).data();

            assertThat(data.candidates().get(0).events())
                    .extracting("code")
                    .containsExactly("rr_offer", "rr_interview_3");
        }
    }

    @Nested
    @DisplayName("Фасет eventCode")
    class Facet {

        @Test
        @DisplayName("все уникальные коды по всем страницам, учитывает поиск по ФИО")
        void facetRespectsSearch() {
            var data = find(request(1, 1, "Иванов Иван", null)).data();

            assertThat(data.eventCode())
                    .extracting(EventCodeDto::codeValue)
                    .containsExactlyInAnyOrder("rr_interview_3", "rr_offer");
        }

        @Test
        @DisplayName("НЕ учитывает фильтр eventCodeList (иначе фильтр нельзя снять в UI)")
        void facetIgnoresCodeFilter() {
            var data = find(request(1, 10, null, List.of("rr_offer"))).data();

            assertThat(data.eventCode())
                    .extracting(EventCodeDto::codeValue)
                    .containsExactlyInAnyOrder("rr_resume_review", "rr_interview_3", "rr_offer");
        }
    }

    @Nested
    @DisplayName("Обогащение заявкой")
    class Enrichment {

        @Test
        @DisplayName("заявка найдена -> заполнена; не найдена -> requisition = null, кандидат не исключён")
        void enrichmentIsOptional() {
            when(requisitionLookupPort.findByVacancyIds(Set.of("v1", "v2", "v3")))
                    .thenReturn(Map.of("v2", new RequisitionShortDto(
                            "guid-2", "12345", "Новая", null, "Департамент технологий", "123456", List.of())));

            var data = find(request(1, 10, null, null)).data();

            assertThat(data.candidates()).hasSize(3);
            assertThat(byName(data.candidates(), "Иванов Иван Иванович").requisition().guid())
                    .isEqualTo("guid-2");
            assertThat(byName(data.candidates(), "Сидоров Пётр Петрович").requisition()).isNull();
        }
    }

    // ===== helpers =====

    private PendingCandidatesResponse find(PendingCandidatesKafkaRequest request) {
        PendingCandidatesResponse response = queryService.find(request);
        assertThat(response.error()).isNull();
        return response;
    }

    private static PendingCandidatesKafkaRequest request(int page, int pageSize,
                                                         String search, List<String> codes) {
        PendingCandidatesFilter filter = (search == null && codes == null)
                ? null : new PendingCandidatesFilter(search, codes);
        return new PendingCandidatesKafkaRequest(page, pageSize, filter, PERNR);
    }

    private static List<String> names(List<PendingCandidateDto> candidates) {
        return candidates.stream().map(PendingCandidateDto::fullName).collect(Collectors.toList());
    }

    private static PendingCandidateDto byName(List<PendingCandidateDto> candidates, String name) {
        return candidates.stream().filter(c -> name.equals(c.fullName())).findFirst().orElseThrow();
    }

    private void save(PendingCandidate candidate) {
        repository.save(candidate);
    }

    private static PendingCandidate candidate(String candidateId, String vacancyId,
                                              String fullName, PendingCandidateEvent... events) {
        return build(PERNR, candidateId, vacancyId, fullName, events);
    }

    private static PendingCandidate other(String candidateId, String vacancyId,
                                          String fullName, PendingCandidateEvent... events) {
        return build(OTHER_PERNR, candidateId, vacancyId, fullName, events);
    }

    private static PendingCandidate build(String pernr, String candidateId, String vacancyId,
                                          String fullName, PendingCandidateEvent... events) {
        PendingCandidate pc = new PendingCandidate();
        pc.setApproverPernr(pernr);
        pc.setCandidateId(candidateId);
        pc.setVacancyId(vacancyId);
        pc.setFullName(fullName);
        for (PendingCandidateEvent event : events) {
            pc.addEvent(event);
        }
        return pc;
    }

    private static PendingCandidateEvent event(String code, Instant statusDate) {
        PendingCandidateEvent e = new PendingCandidateEvent();
        e.setEventCode(code);
        e.setStatusDate(statusDate);
        e.setDays(1);
        e.setExpirationZone(1);
        return e;
    }

    private static Instant t(int day) {
        return Instant.parse("2026-07-0" + day + "T00:00:00Z");
    }
}
