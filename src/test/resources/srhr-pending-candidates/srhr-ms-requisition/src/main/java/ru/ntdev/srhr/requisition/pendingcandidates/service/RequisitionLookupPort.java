package ru.ntdev.srhr.requisition.pendingcandidates.service;

import ru.ntdev.srhr.common.dto.pendingcandidates.RequisitionShortDto;

import java.util.Map;
import java.util.Set;

/**
 * Порт обогащения кандидатов данными заявки на подбор.
 * Реализация адаптирует СУЩЕСТВУЮЩИЙ в srhr-ms-requisition RequisitionRepository:
 * ОДИН batch-запрос findByVacancyIdIn(vacancyIds) (не N+1) + маппинг
 * Requisition -> RequisitionShortDto (guid, number, positionType, staffPosition,
 * structUnitName, structUnitId, structUnitPathList).
 */
public interface RequisitionLookupPort {

    /**
     * @return map vacancyId -> заявка; отсутствующие ключи = заявка не найдена,
     * кандидат при этом отдаётся с requisition = null
     */
    Map<String, RequisitionShortDto> findByVacancyIds(Set<String> vacancyIds);
}
