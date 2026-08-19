package com.pet.requisition.infrastructure.adapter;

import com.pet.requisition.application.model.*;
import com.pet.requisition.application.port.out.*;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryRequestPersistenceAdapter implements RequestPersistencePort {
    private final AtomicLong seq = new AtomicLong();

    public RequestRecord saveRequest(String correlationId, String role, String sessionId, String requestBody, String tabNumber, String status) {
        return new RequestRecord(seq.incrementAndGet(), correlationId, requestBody, role, tabNumber, status);
    }

    public ResponseRecord saveResponse(String correlationId, RequestRecord requestRecord, String responseBody, long processingTime, String code) {
        return new ResponseRecord(seq.incrementAndGet(), correlationId, responseBody, processingTime, code);
    }

    public void updateRequestRole(String c, String r) {
    }

    public void updateRequestTabNumber(String c, String t) {
    }

    public void updateRequestStatus(String c, String s) {
    }

    public void deleteRequest(String c, Long id) {
    }

    public void deleteResponse(String c, Long id) {
    }
    // REAL mapping:
    // entityService.saveAndReturnRequest / saveAndReturnResponse
    // entityService.updateRequestRole / updateRequestTabnum / updateRequestStatus
    // entityService.deleteRequestByUidAndId / deleteResponseByUidAndId
}
