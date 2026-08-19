package com.pet.requisition.application.port.out;
public interface FileStoragePort {
    String getMessage(String adLogin,String sessionId,String traceId,String fileUuid);
    String storeMessage(String adLogin,String sessionId,String traceId,String messageName,String messageContent,String requestUid);
}
