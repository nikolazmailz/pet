package ru.ntdev.srhr.common.audit.sender.requisition;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.ntdev.srhr.common.audit.sender.AuditSender;
import ru.vtb.omni.audit.lib.api.annotation.Audit;
import ru.vtb.omni.audit.lib.api.annotation.AuditParam;

import java.time.LocalDateTime;

@Slf4j
@Service
public class RequisitionPostListSender implements AuditSender {

    private static final RequisitionRequestType REQUEST_TYPE = RequisitionRequestType.REQUISITION_POST_LIST;
    private static final String NOT_FOUND_CODE = "SRHR_REQUISITION_NOT_FOUND_POST_LIST";
    private static final String FORBIDDEN_CODE = "SRHR_REQUISITION_FORBIDDEN_POST_LIST";
    private static final String UNEXPECTED_ERROR_CODE = "SRHR_REQUISITION_UNEXPECTED_ERROR_POST_LIST";

    @Audit(NOT_FOUND_CODE)
    @Override
    public void sendNotFoundEvent(
            @AuditParam(value = "SESSION_ID") String sessionId,
            @AuditParam(value = "SESSION_CREATED_AT") LocalDateTime sessionCreatedAt
    ) {
        log.debug("Отправка события: {}", NOT_FOUND_CODE);
    }

    @Audit(FORBIDDEN_CODE)
    @Override
    public void sendForbiddenEvent(
            @AuditParam(value = "SESSION_ID") String sessionId,
            @AuditParam(value = "SESSION_CREATED_AT") LocalDateTime sessionCreatedAt
    ) {
        log.debug("Отправка события: {}", FORBIDDEN_CODE);
    }

    @Audit(UNEXPECTED_ERROR_CODE)
    @Override
    public void sendUnexpectedErrorEvent(
            @AuditParam(value = "SESSION_ID") String sessionId,
            @AuditParam(value = "SESSION_CREATED_AT") LocalDateTime sessionCreatedAt
    ) {
        log.debug("Отправка события: {}", UNEXPECTED_ERROR_CODE);
    }

    @Override
    public boolean supports(String requestType) {
        return REQUEST_TYPE.value().equals(requestType);
    }

    @Override
    public String eventNotFoundCode() {
        return NOT_FOUND_CODE;
    }

    @Override
    public String eventForbiddenCode() {
        return FORBIDDEN_CODE;
    }

    @Override
    public String eventErrorCode() {
        return UNEXPECTED_ERROR_CODE;
    }
}
