package ru.ntdev.srhr.requisition.pendingcandidates.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class PendingCandidateQueryRepositoryImpl implements PendingCandidateQueryRepository {

    private static final String CODES_EXISTS = """
             and exists (select 1 from pending_candidates_events ef
                         where ef.pending_candidate_id = pc.id
                           and ef.event_code in (:codes))""";

    private final EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public List<Long> findPageIds(String pernr, String search, List<String> eventCodes,
                                  int limit, int offset) {
        String sql = """
                select pc.id
                from pending_candidates pc
                left join pending_candidates_events e on e.pending_candidate_id = pc.id
                """
                + whereClause(search, eventCodes)
                + """

                group by pc.id, pc.full_name
                order by min(e.status_date) asc nulls last, pc.full_name asc, pc.id asc
                limit :limit offset :offset""";

        Query query = entityManager.createNativeQuery(sql)
                .setParameter("limit", limit)
                .setParameter("offset", offset);
        bindFilters(query, pernr, search, eventCodes);

        return ((List<Number>) query.getResultList()).stream()
                .map(Number::longValue)
                .toList();
    }

    @Override
    public long countCandidates(String pernr, String search, List<String> eventCodes) {
        String sql = "select count(*) from pending_candidates pc "
                + whereClause(search, eventCodes);

        Query query = entityManager.createNativeQuery(sql);
        bindFilters(query, pernr, search, eventCodes);

        return ((Number) query.getSingleResult()).longValue();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> findDistinctEventCodes(String pernr, String search) {
        // Фильтр по кодам этапов намеренно не применяется (семантика фасета)
        String sql = """
                select distinct e.event_code
                from pending_candidates_events e
                join pending_candidates pc on pc.id = e.pending_candidate_id
                """
                + whereClause(search, null)
                + "\norder by e.event_code";

        Query query = entityManager.createNativeQuery(sql);
        bindFilters(query, pernr, search, null);

        return query.getResultList();
    }

    private String whereClause(String search, List<String> eventCodes) {
        StringBuilder where = new StringBuilder("where pc.approver_pernr = :pernr");
        if (hasText(search)) {
            where.append(" and pc.full_name ilike :search escape '\\'");
        }
        if (eventCodes != null && !eventCodes.isEmpty()) {
            where.append(CODES_EXISTS);
        }
        return where.toString();
    }

    private void bindFilters(Query query, String pernr, String search, List<String> eventCodes) {
        query.setParameter("pernr", pernr);
        if (hasText(search)) {
            query.setParameter("search", toLikePattern(search));
        }
        if (eventCodes != null && !eventCodes.isEmpty()) {
            query.setParameter("codes", eventCodes);
        }
    }

    /** Экранирование спецсимволов LIKE: пользовательский ввод — литерал, не паттерн. */
    static String toLikePattern(String search) {
        String escaped = search
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
