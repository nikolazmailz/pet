package com.pet.requisition.infrastructure.adapter;


public class DemoFileStorageAdapter implements FileStoragePort {
    public String getMessage(String adLogin, String sessionId, String traceId, String fileUuid) {
        // REAL: filestorageService.getMessageById(adLogin, sessionId, traceId, fileUuid).getMessageContent()
        throw new IllegalStateException("Replace with FilestorageService adapter. fileUuid=" + fileUuid);
    }

    public String storeMessage(String adLogin, String sessionId, String traceId, String messageName, String messageContent, String requestUid) {
        // REAL: filestorageService.putMessageToDb(...).getMessageUID()
        return "demo-file-uuid";
    }
}
