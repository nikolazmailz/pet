package ru.ntdev.srhr.pending.contracts;

import jakarta.validation.constraints.Size;

import java.util.LinkedHashSet;
import java.util.List;

public record PendingCandidatesFilter(
        @Size(max = 255) String search,
        @Size(max = 100) List<@Size(max = 128) String> eventCodeList
) {
    public PendingCandidatesFilter {
        search = normalizeSearch(search);
        eventCodeList = normalizeCodes(eventCodeList);
    }

    public static PendingCandidatesFilter empty() {
        return new PendingCandidatesFilter(null, List.of());
    }

    private static String normalizeSearch(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private static List<String> normalizeCodes(List<String> values) {
        if (values == null || values.isEmpty()) return List.of();
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) result.add(value.trim());
        }
        return List.copyOf(result);
    }
}
