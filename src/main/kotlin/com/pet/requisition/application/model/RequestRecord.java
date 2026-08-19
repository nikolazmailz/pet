package com.pet.requisition.application.model;

public record RequestRecord(
        Long id,
        String uuid,
        String requestBody,
        String functionalRole,
        String tabNumber,
        String status
) {
}
