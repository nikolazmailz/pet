package ru.ntdev.srhr.common.audit.sender.scheduleholiday;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.ntdev.srhr.common.audit.sender.AuditSender;
import ru.vtb.omni.audit.lib.api.annotation.Audit;
import ru.vtb.omni.audit.lib.api.annotation.AuditParam;

import java.time.LocalDateTime;

@Slf4j
@Service
public class ScheduleHolidayPostRequestSender implements AuditSender {

    private static final ScheduleHolidayRequestType REQUEST_TYPE = ScheduleHolidayRequestType.SCHEDULE_HOLIDAY_POST_REQUEST;
    private static final String NOT_FOUND_CODE = "SRHR_SCHEDULE_HOLIDAY_REST_NOT_FOUND_SCHEDULE_HOLIDAY_POST_REQUEST";
    private static final String FORBIDDEN_CODE = "SRHR_SCHEDULE_HOLIDAY_REST_FORBIDDEN_SCHEDULE_HOLIDAY_POST_REQUEST";
    private static final String UNEXPECTED_ERROR_CODE = "SRHR_SCHEDULE_HOLIDAY_REST_UNEXPECTED_ERROR_SCHEDULE_HOLIDAY_POST_REQUEST";

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
