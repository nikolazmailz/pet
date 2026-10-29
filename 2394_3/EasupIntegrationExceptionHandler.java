package ru.ntdev.srhr.ms.easup.integration.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.HttpResponseException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import ru.ntdev.srhr.common.audit.senders.AuditSender;
import ru.ntdev.srhr.common.audit.senders.AuditSenderResolver;
import ru.ntdev.srhr.common.dto.api.error.ErrorResponse;
import ru.ntdev.srhr.common.dto.api.error.ServiceError;
import ru.ntdev.srhr.common.dto.api.error.ServiceErrorDto;
import ru.ntdev.srhr.common.dto.sap.SapErrorResponse;
import ru.ntdev.srhr.common.util.LogUtil;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
class EasupIntegrationExceptionHandler {

    private static final String ATTR_REQUEST_TYPE = "requestType";
    private static final String ATTR_SESSION_ID = "sessionId";
    private static final String ATTR_PERNR = "pernr";
    private static final String ATTR_ADLOGIN = "adlogin";
    private static final String ATTR_TRACE_ID = "traceId";
    private static final String UNDEFINED = "undefined";

    private static final String CODE_INTERNAL = "500";
    private static final String CODE_NPE = "NPE";
    private static final int STATUS_NOT_EXTENDED = 510;

    /** Маркер того, что в сообщении исключения лежит JSON ошибки SAP. */
    private static final String SAP_ERROR_MARKER = "{\"error\"";
    private static final String SAP_MESSAGE_MARKER = "SAP";

    private final AuditSenderResolver auditSenderResolver;
    private final ObjectMapper objectMapper;

    // ------------------------------------------------------------------ handlers

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> defaultErrorHandler(HttpServletRequest req, Exception e) {
        AuditContext ctx = AuditContext.from(req);
        ServiceErrorDto dto = resolveError(e.getMessage(), CODE_INTERNAL, ctx.traceId());

        LogUtil.logSapErrorResponse(ctx.pernr(), ctx.sessionId(), e.getMessage(), ctx.traceId(), null);
        sendAudit(ctx, AuditEvent.UNEXPECTED_ERROR, dto.getError().getMessage());
        return new ResponseEntity<>(dto, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Object> nullPointerExceptionHandler(HttpServletRequest req, NullPointerException e) {
        AuditContext ctx = AuditContext.from(req);
        ServiceErrorDto dto = new ServiceErrorDto()
                .setError(new ServiceError(ErrorResponse.EASUP_RETURNED_NULL, CODE_NPE, ctx.traceId(), new ArrayList<>()));

        sendAudit(ctx, AuditEvent.NOT_FOUND, ErrorResponse.EASUP_RETURNED_NULL);
        return new ResponseEntity<>(dto, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<Object> httpServerErrorHandler(HttpServletRequest req, HttpServerErrorException e) {
        AuditContext ctx = AuditContext.from(req);
        int status = e.getStatusCode().value();
        ServiceErrorDto dto = resolveError(e.getMessage(), String.valueOf(e.getStatusCode()), ctx.traceId());

        LogUtil.logSapErrorResponse(ctx.pernr(), ctx.sessionId(), e.getMessage(), ctx.traceId(), status);
        if (status != STATUS_NOT_EXTENDED) {
            sendAudit(ctx, AuditEvent.UNEXPECTED_ERROR, dto.getError().getMessage());
        }
        return new ResponseEntity<>(dto, e.getStatusCode());
    }

    /** Ошибка от Apache HttpClient (EasupClient). */
    @ExceptionHandler(HttpResponseException.class)
    public ResponseEntity<Object> httpResponseErrorHandler(HttpServletRequest req, HttpResponseException e) {
        return handleHttpStatus(req, e.getStatusCode(), String.valueOf(e.getStatusCode()), e.getMessage());
    }

    /** Ошибка от Spring RestTemplate. */
    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<Object> httpClientErrorHandler(HttpServletRequest req, HttpClientErrorException e) {
        return handleHttpStatus(req, e.getStatusCode().value(), e.getStatusCode().toString(), e.getMessage());
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Object> validationMethodErrorHandler(HandlerMethodValidationException e) {
        String errorText = e.getAllValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .map(MessageSourceResolvable::getDefaultMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
        ServiceError error = new ServiceError(errorText, String.valueOf(e.getStatusCode()), UNDEFINED, new ArrayList<>());
        return new ResponseEntity<>(new ServiceErrorDto(error), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    // ------------------------------------------------------------------ shared flow

    /** Общая обработка 4xx от ЕАСУП: 401/403 → forbidden, 404 → not found, остальное — только лог. */
    private ResponseEntity<Object> handleHttpStatus(HttpServletRequest req, int status, String code, String message) {
        AuditContext ctx = AuditContext.from(req);
        ServiceErrorDto dto = resolveError(message, code, ctx.traceId());

        LogUtil.logSapErrorResponse(ctx.pernr(), ctx.sessionId(), message, ctx.traceId(), status);

        if (status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.FORBIDDEN.value()) {
            sendAudit(ctx, AuditEvent.FORBIDDEN, dto.getError().getMessage());
        } else if (status == HttpStatus.NOT_FOUND.value()) {
            sendAudit(ctx, AuditEvent.NOT_FOUND, dto.getError().getMessage());
            dto = new ServiceErrorDto()
                    .setError(new ServiceError(ErrorResponse.EASUP_REQUEST_NOT_FOUND, code, ctx.traceId(), new ArrayList<>()));
        } else {
            logFailure(ctx, dto.getError().getMessage());
        }
        return new ResponseEntity<>(dto, HttpStatusCode.valueOf(status));
    }

    // ------------------------------------------------------------------ error dto

    private ServiceErrorDto resolveError(String message, String code, String traceId) {
        if (message != null && message.contains(SAP_MESSAGE_MARKER)) {
            return parseSapError(message);
        }
        return new ServiceErrorDto().setError(new ServiceError(message, code, traceId, new ArrayList<>()));
    }

    /** Достаёт из сообщения исключения JSON вида {"error": {...}}, который вернул SAP. */
    private ServiceErrorDto parseSapError(String message) {
        try {
            String json = message.substring(message.indexOf(SAP_ERROR_MARKER));
            SapErrorResponse sapError = objectMapper.readValue(json, SapErrorResponse.class);
            ServiceError error = new ServiceError();
            error.setMessage(sapError.getError().getMessage());
            error.setDetails(sapError.getError().getDetails());
            return new ServiceErrorDto(error);
        } catch (Exception e) {
            log.warn("Не удалось разобрать ошибку SAP: {}", e.getMessage());
            return new ServiceErrorDto();
        }
    }

    // audit

    private record AuditContext(String requestType, String sessionId, String pernr, String adlogin,
                                String traceId, LocalDateTime sessionCreatedAt) {

        static AuditContext from(HttpServletRequest req) {
            Object requestType = req.getAttribute(ATTR_REQUEST_TYPE);
            Object sessionId = req.getAttribute(ATTR_SESSION_ID);
            Object pernr = req.getAttribute(ATTR_PERNR);
            Object adlogin = req.getAttribute(ATTR_ADLOGIN);
            Object traceId = req.getAttribute(ATTR_TRACE_ID);

            if (requestType == null || sessionId == null || pernr == null || adlogin == null || traceId == null) {
                log.warn(ErrorResponse.ERROR_NOT_ALL_REQUIRED_AUDIT_PARAMS_MESSAGE);
            }
            return new AuditContext(
                    orUndefined(requestType),
                    orUndefined(sessionId),
                    orUndefined(pernr),
                    orUndefined(adlogin),
                    orUndefined(traceId),
                    LocalDateTime.now());
        }

        private static String orUndefined(Object value) {
            return value != null ? value.toString() : UNDEFINED;
        }
    }

    @FunctionalInterface
    private interface AuditAction {
        void send(AuditSender sender, LocalDateTime sessionCreatedAt, String pernr, String sessionId, String adlogin)
                throws Exception;
    }

    private enum AuditEvent {
        UNEXPECTED_ERROR(AuditSender::sendUnexpectedErrorEvent, AuditSender::eventErrorCode),
        FORBIDDEN(AuditSender::sendForbiddenEvent, AuditSender::eventForbiddenCode),
        NOT_FOUND(AuditSender::sendNotFoundEvent, AuditSender::eventNotFoundCode);

        final AuditAction action;
        final Function<AuditSender, String> code;

        AuditEvent(AuditAction action, Function<AuditSender, String> code) {
            this.action = action;
            this.code = code;
        }
    }

    private void sendAudit(AuditContext ctx, AuditEvent event, String errorText) {
        logFailure(ctx, errorText);
        AuditSender sender = auditSenderResolver.resolve(ctx.requestType());
        if (sender == null) {
            return;
        }
        try {
            event.action.send(sender, ctx.sessionCreatedAt(), ctx.pernr(), ctx.sessionId(), ctx.adlogin());
        } catch (Exception ex) {
            log.error("Ошибка отправки события [{}] в аудит: [{}]", event.code.apply(sender), ex.getMessage());
        }
    }

    private static void logFailure(AuditContext ctx, String errorText) {
        log.error("Неуспешное выполнение эндпоинта [{}] для юзера: [{}]. Ошибка: [{}]",
                ctx.requestType(), ctx.pernr(), errorText);
    }
}
