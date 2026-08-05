package ru.ntdev.srhr.common.audit.sender.timesheet;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.ntdev.srhr.common.audit.sender.AuditSender;
import ru.vtb.omni.audit.lib.api.annotation.Audit;
import ru.vtb.omni.audit.lib.api.annotation.AuditParam;

import java.time.LocalDateTime;

@Slf4j
@Service
public class TimesheetReferenceEventSender implements AuditSender {

    private static final String REQUEST_TYPE = "timesheet_reference_event";
    private static final String NOT_FOUND_CODE = "SRHR_TIMESHEET_REST_NOT_FOUND_TIMESHEET_REFERENCE_EVENT";
    private static final String FORBIDDEN_CODE = "SRHR_TIMESHEET_REST_FORBIDDEN_TIMESHEET_REFERENCE_EVENT";
    private static final String UNEXPECTED_ERROR_CODE = "SRHR_TIMESHEET_REST_UNEXPECTED_ERROR_TIMESHEET_REFERENCE_EVENT";

    @Audit(NOT_FOUND_CODE)
    @Override
    public void sendNotFoundEvent(
            @AuditParam(value = SESSION_ID) String sessionId,
            @AuditParam(value = SESSION_CREATED_AT) LocalDateTime sessionCreatedAt
    ) {
        log.debug("Отправка события: {}", NOT_FOUND_CODE);
    }

    @Audit(FORBIDDEN_CODE)
    @Override
    public void sendForbiddenEvent(
            @AuditParam(value = SESSION_ID) String sessionId,
            @AuditParam(value = SESSION_CREATED_AT) LocalDateTime sessionCreatedAt
    ) {
        log.debug("Отправка события: {}", FORBIDDEN_CODE);
    }

    @Audit(UNEXPECTED_ERROR_CODE)
    @Override
    public void sendUnexpectedErrorEvent(
            @AuditParam(value = SESSION_ID) String sessionId,
            @AuditParam(value = SESSION_CREATED_AT) LocalDateTime sessionCreatedAt
    ) {
        log.debug("Отправка события: {}", UNEXPECTED_ERROR_CODE);
    }

    @Override
    public boolean supports(String requestType) {
        return REQUEST_TYPE.equals(requestType);
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
