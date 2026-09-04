package ru.ntdev.srhr.requisition.pendingcandidates.repository;

import java.util.List;

/**
 * Кастомные запросы выборки. Все три метода используют одинаковое WHERE-ядро:
 * approver_pernr + опциональный поиск по ФИО + опциональный фильтр по кодам этапов
 * (кроме фасета — он без фильтра по кодам, см. javadoc метода).
 */
public interface PendingCandidateQueryRepository {

    /**
     * Идентификаторы кандидатов страницы. Сортировка: ближайший дедлайн
     * (MIN(status_date)) ASC NULLS LAST, затем full_name ASC, затем id ASC
     * (стабильность пагинации).
     */
    List<Long> findPageIds(String pernr, String search, List<String> eventCodes, int limit, int offset);

    /** Всего кандидатов с учётом всех фильтров (поле count ответа). */
    long countCandidates(String pernr, String search, List<String> eventCodes);

    /**
     * Фасет: уникальные коды этапов по всем кандидатам пользователя.
     * Учитывает поиск по ФИО, НЕ учитывает фильтр eventCodeList — иначе
     * пользователь не сможет снять фильтр в UI.
     */
    List<String> findDistinctEventCodes(String pernr, String search);
}
