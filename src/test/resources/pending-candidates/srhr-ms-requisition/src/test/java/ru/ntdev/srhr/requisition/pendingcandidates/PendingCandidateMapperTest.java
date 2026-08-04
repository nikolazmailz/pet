package ru.ntdev.srhr.requisition.pendingcandidates;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.ntdev.srhr.common.dto.pendingcandidates.estaff.EstaffCandidate;
import ru.ntdev.srhr.common.dto.pendingcandidates.estaff.EstaffCandidateEvent;
import ru.ntdev.srhr.requisition.pendingcandidates.entity.PendingCandidate;
import ru.ntdev.srhr.requisition.pendingcandidates.mapper.PendingCandidateMapper;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PendingCandidateMapper")
class PendingCandidateMapperTest {

    private final PendingCandidateMapper mapper = Mappers.getMapper(PendingCandidateMapper.class);

    @Test
    @DisplayName("маппит кандидата с событиями: pernr, millis -> Instant, back-reference")
    void mapsCandidateWithEvents() {
        var estaff = new EstaffCandidate("1234567890123456789", "9876543210987654321",
                "Иванов Иван Иванович",
                List.of(new EstaffCandidateEvent("rr_interview_3", 1701565600000L, 3, 2)));

        PendingCandidate entity = mapper.toEntity(estaff, "12345678");

        assertThat(entity.getId()).isNull();
        assertThat(entity.getApproverPernr()).isEqualTo("12345678");
        assertThat(entity.getCandidateId()).isEqualTo("1234567890123456789");
        assertThat(entity.getVacancyId()).isEqualTo("9876543210987654321");
        assertThat(entity.getEvents()).singleElement().satisfies(event -> {
            assertThat(event.getEventCode()).isEqualTo("rr_interview_3");
            assertThat(event.getStatusDate()).isEqualTo(Instant.ofEpochMilli(1701565600000L));
            assertThat(event.getDays()).isEqualTo(3);
            assertThat(event.getExpirationZone()).isEqualTo(2);
            assertThat(event.getCandidate()).isSameAs(entity);
        });
    }

    @Test
    @DisplayName("null-список events нормализуется в пустой")
    void nullEventsNormalized() {
        var estaff = new EstaffCandidate("1", "2", "Иванов", null);

        PendingCandidate entity = mapper.toEntity(estaff, "12345678");

        assertThat(entity.getEvents()).isNotNull().isEmpty();
    }
}
