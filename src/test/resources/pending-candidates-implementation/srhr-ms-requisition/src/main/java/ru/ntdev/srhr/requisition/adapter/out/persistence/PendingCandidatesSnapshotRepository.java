package ru.ntdev.srhr.requisition.adapter.out.persistence;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.ntdev.srhr.pending.contracts.PendingCandidateEventSnapshot;
import ru.ntdev.srhr.pending.contracts.PendingCandidateSnapshot;
import ru.ntdev.srhr.requisition.domain.PendingCandidatesPersistenceException;

import java.sql.Timestamp;

@Repository
public class PendingCandidatesSnapshotRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public PendingCandidatesSnapshotRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void lockApprover(String pernr) {
        jdbc.query(
                "select pg_advisory_xact_lock(hashtextextended(:pernr, 0))",
                new MapSqlParameterSource("pernr", pernr),
                rs -> { /* блокировка удерживается до завершения транзакции */ }
        );
    }

    public void deleteByApprover(String pernr) {
        jdbc.update("delete from pending_candidates where approver_pernr = :pernr",
                new MapSqlParameterSource("pernr", pernr));
    }

    public long insertCandidate(String pernr, PendingCandidateSnapshot candidate) {
        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("candidateId", candidate.candidateId())
                    .addValue("vacancyId", candidate.vacancyId())
                    .addValue("fullName", candidate.fullName())
                    .addValue("pernr", pernr);
            jdbc.update("""
                    insert into pending_candidates(candidate_id, vacancy_id, full_name, approver_pernr)
                    values (:candidateId, :vacancyId, :fullName, :pernr)
                    """, params, keyHolder, new String[]{"id"});
            Number key = keyHolder.getKey();
            if (key == null) throw new IllegalStateException("База данных не вернула id кандидата");
            return key.longValue();
        } catch (DataAccessException | IllegalStateException ex) {
            throw new PendingCandidatesPersistenceException("Не удалось сохранить кандидата", ex);
        }
    }

    public void insertEvent(long candidatePk, PendingCandidateEventSnapshot event) {
        try {
            jdbc.update("""
                    insert into pending_candidates_events(
                        pending_candidate_id, event_code, status_date, days, expiration_zone
                    ) values (
                        :candidateId, :eventCode, :statusDate, :days, :expirationZone
                    )
                    """, new MapSqlParameterSource()
                    .addValue("candidateId", candidatePk)
                    .addValue("eventCode", event.eventCode())
                    .addValue("statusDate", event.statusDate() == null ? null : Timestamp.from(event.statusDate()))
                    .addValue("days", event.days())
                    .addValue("expirationZone", event.expirationZone()));
        } catch (DataAccessException ex) {
            throw new PendingCandidatesPersistenceException("Не удалось сохранить этап кандидата", ex);
        }
    }
}
