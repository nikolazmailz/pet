package ru.ntdev.srhr.common.dto.pendingcandidates;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * @param count     ВСЕГО кандидатов с учётом фильтров (не размер страницы)
 * @param eventCode фасет: все уникальные коды этапов по всем страницам;
 *                  учитывает поиск по ФИО, но НЕ учитывает фильтр eventCodeList —
 *                  иначе пользователь не сможет снять фильтр в UI
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PendingCandidatesData(
        Integer page,
        Integer pageSize,
        Long count,
        List<EventCodeDto> eventCode,
        List<PendingCandidateDto> candidates) {
}
