package ru.ntdev.srhr.common.dto.pendingcandidates;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * @param search        свободный поиск по ФИО, полное вхождение (ILIKE %...%)
 * @param eventCodeList фильтр по кодам этапов, условие ИЛИ
 */
public record PendingCandidatesFilter(
        @Size(max = 255) String search,
        @Size(max = 50) List<@NotBlank String> eventCodeList) {
}
