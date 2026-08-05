package ru.ntdev.srhr.common.audit.sender.jwt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.vtb.omni.audit.lib.api.annotation.Audit;
import ru.vtb.omni.audit.lib.api.annotation.AuditParam;

@Slf4j
@Service
public class JwtAuditSenderImpl implements JwtAuditSender {

    private static final String JWT_VALIDATE_CODE = "SRHR_JWT_VALIDATE";
    private static final String JWT_FORBIDDEN_CODE = "SRHR_FORBIDDEN_INVALID_JWT";
    private static final String JWT_UNEXPECTED_ERROR_CODE = "SRHR_JWT_VALIDATE_UNEXPECTED_ERROR";

    @Audit(JWT_VALIDATE_CODE)
    @Override
    public void sendJwtValidateEvent(
            @AuditParam(value = SESSION_ID) String sessionId,
            Boolean isValid,
            String errorMessage
    ) throws Exception {
        log.debug("Отправка события: {}", JWT_VALIDATE_CODE);
        if (!isValid) throw new Exception(errorMessage);
    }

    @Audit(JWT_FORBIDDEN_CODE)
    @Override
    public void sendJwtForbiddenEvent(
            @AuditParam(value = SESSION_ID) String sessionId
    ) {
        log.debug("Отправка события: {}", JWT_FORBIDDEN_CODE);
    }

    @Audit(JWT_UNEXPECTED_ERROR_CODE)
    @Override
    public void sendJwtUnexpectedErrorEvent(
            @AuditParam(value = SESSION_ID) String sessionId
    ) {
        log.debug("Отправка события: {}", JWT_UNEXPECTED_ERROR_CODE);
    }
}
