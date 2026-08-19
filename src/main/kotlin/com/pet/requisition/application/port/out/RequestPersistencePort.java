package com.pet.requisition.application.port.out;


import com.pet.requisition.application.model.*;

public interface RequestPersistencePort {
    RequestRecord saveRequest(String correlationId, String role, String sessionId, String requestBody, String tabNumber, String status);

    ResponseRecord saveResponse(String correlationId, RequestRecord requestRecord, String responseBody, long processingTime, String code);

    void updateRequestRole(String correlationId, String role);

    void updateRequestTabNumber(String correlationId, String tabNumber);

    void updateRequestStatus(String correlationId, String status);

    void deleteRequest(String correlationId, Long requestId);

    void deleteResponse(String correlationId, Long responseId);
}
