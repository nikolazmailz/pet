package ru.ntdev.srhr.common.audit.sender.jwt;

public interface JwtAuditSender {

    String SESSION_ID = "sessionId";

    void sendJwtValidateEvent(
            String sessionId,
            Boolean isValid,
            String errorMessage
    ) throws Exception;

    void sendJwtForbiddenEvent(
            String sessionId
    );

    void sendJwtUnexpectedErrorEvent(
            String sessionId
    );
}
