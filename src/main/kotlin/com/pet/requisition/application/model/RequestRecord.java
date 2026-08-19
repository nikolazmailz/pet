package com.pet.requisition.application.model;
public record RequestRecord(Long id,String correlationId,String requestBody,String functionalRole,String tabNumber,String status) {}
