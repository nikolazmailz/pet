package ru.ntdev.srhr.requisition.pendingcandidates.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import ru.ntdev.srhr.common.dto.pendingcandidates.estaff.EstaffCandidate;
import ru.ntdev.srhr.common.dto.pendingcandidates.estaff.EstaffCandidateEvent;
import ru.ntdev.srhr.requisition.pendingcandidates.entity.PendingCandidate;
import ru.ntdev.srhr.requisition.pendingcandidates.entity.PendingCandidateEvent;

import java.time.Instant;
import java.util.ArrayList;

@Mapper(componentModel = "spring")
public interface PendingCandidateMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "approverPernr", source = "pernr")
    @Mapping(target = "events", source = "candidate.events")
    PendingCandidate toEntity(EstaffCandidate candidate, String pernr);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "candidate", ignore = true)
    @Mapping(target = "statusDate", source = "statusDate", qualifiedByName = "millisToInstant")
    PendingCandidateEvent toEventEntity(EstaffCandidateEvent event);

    @Named("millisToInstant")
    default Instant millisToInstant(Long epochMillis) {
        return epochMillis == null ? null : Instant.ofEpochMilli(epochMillis);
    }

    /** Восстановление back-reference и нормализация null-списка от Е-стафф. */
    @AfterMapping
    default void linkEvents(@MappingTarget PendingCandidate target) {
        if (target.getEvents() == null) {
            target.setEvents(new ArrayList<>());
            return;
        }
        target.getEvents().forEach(e -> e.setCandidate(target));
    }
}
