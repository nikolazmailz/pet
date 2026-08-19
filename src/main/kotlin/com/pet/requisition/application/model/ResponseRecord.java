package com.pet.requisition.application.model;
public record ResponseRecord(Long id,String correlationId,String responseBody,long processingTime,String code) {}
