package ru.ntdev.srhr.requisition.adapter.out.persistence;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.ntdev.srhr.pending.contracts.PendingCandidatesPageRequest;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class PendingCandidatesQueryRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public PendingCandidatesQueryRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<PendingCandidateRow> findPage(String pernr, PendingCandidatesPageRequest request) {
        QueryParts parts = buildWhere(pernr, request);
        String sql = """
                select pc.id, pc.candidate_id, pc.vacancy_id, pc.full_name
                from pending_candidates pc
                """ + parts.where + """
                order by lower(pc.full_name) asc, pc.id asc
                limit :limit offset :offset
                """;
        parts.params.addValue("limit", request.pageSize()).addValue("offset", request.offset());
        return jdbc.query(sql, parts.params, (rs, rowNum) -> new PendingCandidateRow(
                rs.getLong("id"),
                rs.getString("candidate_id"),
                rs.getString("vacancy_id"),
                rs.getString("full_name")
        ));
    }

    public long count(String pernr, PendingCandidatesPageRequest request) {
        QueryParts parts = buildWhere(pernr, request);
        Long result = jdbc.queryForObject("select count(*) from pending_candidates pc " + parts.where,
                parts.params, Long.class);
        return result == null ? 0 : result;
    }

    /**
     * Все коды событий по кандидатам, прошедшим весь набор фильтров. Сам выбранный
     * eventCodeList определяет множество кандидатов, но после этого возвращаются все их коды.
     */
    public List<String> findAvailableEventCodes(String pernr, PendingCandidatesPageRequest request) {
        QueryParts parts = buildWhere(pernr, request);
        String sql = """
                select distinct events.event_code
                from pending_candidates_events events
                join pending_candidates pc on pc.id = events.pending_candidate_id
                """ + parts.where + " order by events.event_code asc";
        return jdbc.query(sql, parts.params, (rs, rowNum) -> rs.getString("event_code"));
    }

    public Map<Long, List<PendingCandidateEventRow>> findEvents(List<Long> candidateIds) {
        if (candidateIds.isEmpty()) return Map.of();
        List<PendingCandidateEventRow> rows = jdbc.query("""
                select id, pending_candidate_id, event_code, status_date, days, expiration_zone
                from pending_candidates_events
                where pending_candidate_id in (:ids)
                order by pending_candidate_id asc, status_date asc, id asc
                """, new MapSqlParameterSource("ids", candidateIds), (rs, rowNum) -> {
            Timestamp timestamp = rs.getTimestamp("status_date");
            return new PendingCandidateEventRow(
                    rs.getLong("id"),
                    rs.getLong("pending_candidate_id"),
                    rs.getString("event_code"),
                    timestamp == null ? null : timestamp.toInstant(),
                    (Integer) rs.getObject("days"),
                    (Integer) rs.getObject("expiration_zone")
            );
        });
        Map<Long, List<PendingCandidateEventRow>> result = new LinkedHashMap<>();
        for (PendingCandidateEventRow row : rows) {
            result.computeIfAbsent(row.pendingCandidateId(), ignored -> new ArrayList<>()).add(row);
        }
        return result;
    }

    private QueryParts buildWhere(String pernr, PendingCandidatesPageRequest request) {
        StringBuilder where = new StringBuilder(" where pc.approver_pernr = :pernr ");
        MapSqlParameterSource params = new MapSqlParameterSource("pernr", pernr);
        if (request.filter().search() != null) {
            where.append(" and pc.full_name ilike :search escape '!' ");
            params.addValue("search", "%" + escapeLike(request.filter().search()) + "%");
        }
        if (!request.filter().eventCodeList().isEmpty()) {
            where.append("""
                     and exists (
                         select 1
                         from pending_candidates_events event_filter
                         where event_filter.pending_candidate_id = pc.id
                           and event_filter.event_code in (:eventCodes)
                     )
                    """);
            params.addValue("eventCodes", request.filter().eventCodeList());
        }
        return new QueryParts(where.toString(), params);
    }

    private String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    private record QueryParts(String where, MapSqlParameterSource params) {}
}
