package ru.ntdev.srhr.requisition.pendingcandidates.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.ntdev.srhr.requisition.pendingcandidates.entity.PendingCandidate;

import java.util.Collection;
import java.util.List;

public interface PendingCandidateRepository
        extends JpaRepository<PendingCandidate, Long>, PendingCandidateQueryRepository {

    /**
     * Bulk-delete обходит каскады JPA, но дочерние pending_candidates_events
     * удаляет БД через FK ON DELETE CASCADE (см. миграцию).
     */
    @Modifying(flushAutomatically = true)
    @Query("delete from PendingCandidate pc where pc.approverPernr = :pernr")
    void deleteByApproverPernr(@Param("pernr") String pernr);

    /**
     * Fetch кандидатов страницы с событиями. Порядок ids восстанавливается
     * в сервисе — SQL IN порядок не гарантирует.
     */
    @Query("select distinct pc from PendingCandidate pc left join fetch pc.events where pc.id in :ids")
    List<PendingCandidate> findWithEventsByIdIn(@Param("ids") Collection<Long> ids);
}
