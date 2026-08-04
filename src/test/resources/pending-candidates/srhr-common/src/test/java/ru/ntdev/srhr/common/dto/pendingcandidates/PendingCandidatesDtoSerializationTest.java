package ru.ntdev.srhr.common.dto.pendingcandidates;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import ru.ntdev.srhr.common.dto.pendingcandidates.estaff.EstaffPendingCandidatesRequest;
import ru.ntdev.srhr.common.dto.pendingcandidates.estaff.EstaffPendingCandidatesResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PendingCandidatesDtoSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("Контракт Е-стафф (SAPPO)")
    class EstaffContract {

        @Test
        @DisplayName("числовые candidateId/vacancyId десериализуются в String без потери точности")
        void numericIdsDeserializeToString() throws Exception {
            String json = """
                    {
                      "data": {
                        "candidates": [{
                          "candidateId": 1234567890123456789,
                          "vacancyId": 1234567890123456789,
                          "fullName": "Иванов Иван Иванович",
                          "events": [{
                            "eventCode": "rr_interview_3",
                            "statusDate": 1701565600000,
                            "days": 3,
                            "expirationZone": 2
                          }]
                        }]
                      }
                    }
                    """;

            EstaffPendingCandidatesResponse response =
                    objectMapper.readValue(json, EstaffPendingCandidatesResponse.class);

            var candidate = response.data().candidates().get(0);
            assertThat(candidate.candidateId()).isEqualTo("1234567890123456789");
            assertThat(candidate.vacancyId()).isEqualTo("1234567890123456789");
            assertThat(candidate.events().get(0).statusDate()).isEqualTo(1701565600000L);
        }

        @Test
        @DisplayName("неизвестные поля не ломают десериализацию")
        void unknownFieldsAreIgnored() throws Exception {
            String json = """
                    { "data": { "candidates": [], "totallyNewSapField": 42 }, "meta": {} }
                    """;

            EstaffPendingCandidatesResponse response =
                    objectMapper.readValue(json, EstaffPendingCandidatesResponse.class);

            assertThat(response.data().candidates()).isEmpty();
        }

        @Test
        @DisplayName("запрос сериализуется с обёрткой request")
        void requestHasWrapperObject() throws Exception {
            String json = objectMapper.writeValueAsString(EstaffPendingCandidatesRequest.of("12345678"));

            assertThat(objectMapper.readTree(json).at("/request/pernr").asText())
                    .isEqualTo("12345678");
        }
    }

    @Nested
    @DisplayName("Ответ фронту")
    class FrontContract {

        @Test
        @DisplayName("round-trip успешного ответа, error отсутствует в JSON (NON_NULL)")
        void okResponseRoundTrip() throws Exception {
            var data = new PendingCandidatesData(1, 20, 2L,
                    List.of(new EventCodeDto("rr_resume_review")),
                    List.of(new PendingCandidateDto("1", "2", "Иванов Иван Иванович",
                            new RequisitionShortDto("guid", "12345", "Новая",
                                    new StaffPositionDto("1234567", "Аналитик"),
                                    "Департамент технологий", "123456",
                                    List.of(new StructUnitPathDto("12314", "Управление", "Департамент технологий"))),
                            List.of(new PendingCandidateEventDto("rr_resume_review", 2, 1)))));

            String json = objectMapper.writeValueAsString(PendingCandidatesResponse.ok(data));

            assertThat(json).doesNotContain("\"error\"");
            PendingCandidatesResponse restored = objectMapper.readValue(json, PendingCandidatesResponse.class);
            assertThat(restored).isEqualTo(PendingCandidatesResponse.ok(data));
        }

        @Test
        @DisplayName("ответ с ошибкой не содержит data")
        void errorResponseHasNoData() throws Exception {
            String json = objectMapper.writeValueAsString(
                    PendingCandidatesResponse.error(PendingCandidatesError.ESTAFF_UNAVAILABLE, "timeout"));

            assertThat(json).doesNotContain("\"data\"");
            assertThat(objectMapper.readTree(json).at("/error/code").asText())
                    .isEqualTo("ESTAFF_UNAVAILABLE");
        }

        @Test
        @DisplayName("requisition = null у кандидата не сериализуется (NON_NULL)")
        void nullRequisitionOmitted() throws Exception {
            var dto = new PendingCandidateDto("1", "2", "Иванов", null, List.of());

            assertThat(objectMapper.writeValueAsString(dto)).doesNotContain("requisition");
        }
    }

    @Nested
    @DisplayName("Kafka-запрос")
    class KafkaRequest {

        @Test
        @DisplayName("строится из web-запроса с добавлением pernr")
        void builtFromWebRequest() {
            var web = new PendingCandidatesWebRequest(2, 50,
                    new PendingCandidatesFilter("Иванов", List.of("rr_interview_3")));

            var kafka = PendingCandidatesKafkaRequest.from(web, "12345678");

            assertThat(kafka.page()).isEqualTo(2);
            assertThat(kafka.pageSize()).isEqualTo(50);
            assertThat(kafka.filter()).isEqualTo(web.filter());
            assertThat(kafka.pernr()).isEqualTo("12345678");
        }
    }
}
