package ru.ntdev.srhr.ms.delegation.management.rest.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.HttpResponseException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import ru.ntdev.srhr.common.audit.senders.AuditSender;
import ru.ntdev.srhr.common.audit.senders.AuditSenderResolver;
import ru.ntdev.srhr.common.dto.api.error.ErrorResponse;
import ru.ntdev.srhr.common.dto.api.error.SrhrError;
import ru.ntdev.srhr.common.dto.api.error.SrhrErrorDetail;
import ru.ntdev.srhr.common.dto.api.error.SrhrErrorDto;
import ru.ntdev.srhr.common.util.LogUtil;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ru.ntdev.srhr.common.dto.api.error.ErrorResponse.REST_RETURNED_NULL;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class DelegationManagementExceptionHandler {

    private static final String ATTR_REQUEST_TYPE = "requestType";
    private static final String ATTR_SESSION_ID = "sessionId";
    private static final String ATTR_SESSION_CREATED_AT = "sessionCreatedAt";
    private static final String ATTR_TRACE_ID = "traceId";
    private static final String UNDEFINED = "undefined";

    private static final String CODE_ARG_NOT_VALID = "422_ARG_NOT_VALID";
    private static final String CODE_VALIDATION_ERROR = "422_VALIDATION_ERROR";
    private static final String CODE_MISSING_PARAM = "400_MISSING_PARAM";
    private static final String CODE_HANDLER_EXCEPTION = "ERROR_HANDLER_EXCEPTION";

    /** Маркер того, что в сообщении исключения лежит JSON ошибки нижестоящего сервиса. */
    private static final String RESENT_ERROR_MARKER = "{\"error\"";

    private final AuditSenderResolver auditSenderResolver;
    private final ObjectMapper objectMapper;

    // ------------------------------------------------------------------ handlers

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> defaultErrorHandler(HttpServletRequest req, Exception e) {
        AuditContext ctx = AuditContext.from(req);
        SrhrErrorDto dto = resolveError(e.getMessage(), ctx.traceId());
        sendAudit(ctx, AuditEvent.UNEXPECTED_ERROR, dto.getError().getMessage());
        return buildErrorResponse(dto, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Object> nullPointerExceptionHandler(HttpServletRequest req, NullPointerException e) {
        AuditContext ctx = AuditContext.from(req);
        sendAudit(ctx, AuditEvent.UNEXPECTED_ERROR, REST_RETURNED_NULL);
        return buildErrorResponse(SrhrErrorDto.of(REST_RETURNED_NULL, "", ctx.traceId()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(HttpResponseException.class)
    public ResponseEntity<Object> httpResponseErrorHandler(HttpServletRequest req, HttpResponseException e) {
        AuditContext ctx = AuditContext.from(req);
        int status = e.getStatusCode();

        String message = e.getReasonPhrase() != null ? e.getReasonPhrase() : e.getMessage();
        SrhrErrorDto dto = isResentError(e.getMessage())
                ? parseResentError(e.getMessage(), ctx.traceId())
                : SrhrErrorDto.of(message, "", ctx.traceId());

        if (status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.FORBIDDEN.value()) {
            sendAudit(ctx, AuditEvent.FORBIDDEN, null);
        } else if (status == HttpStatus.NOT_FOUND.value()) {
            sendAudit(ctx, AuditEvent.NOT_FOUND, null);
            if (e.getMessage() == null) {
                // TODO: PAYSTUB_NOT_FOUND в сервисе делегирования — похоже на копипасту из paystub.
                dto = SrhrErrorDto.of(ErrorResponse.PAYSTUB_NOT_FOUND, "", ctx.traceId());
            }
        }
        return buildErrorResponse(dto, HttpStatusCode.valueOf(status));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> validationArgumentErrorHandler(MethodArgumentNotValidException e) {
        String errorText = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
        return buildErrorResponse(SrhrErrorDto.of(errorText, CODE_ARG_NOT_VALID, null), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Object> validationErrorHandler(ValidationException e) {
        return buildErrorResponse(SrhrErrorDto.of(e.getMessage(), CODE_VALIDATION_ERROR, null), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Object> validationMethodErrorHandler(HandlerMethodValidationException e) {
        String errorText = e.getAllValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .map(MessageSourceResolvable::getDefaultMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
        return buildErrorResponse(SrhrErrorDto.of(errorText, CODE_VALIDATION_ERROR, null), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Object> missingServletRequestParameterExceptionHandler(MissingServletRequestParameterException e) {
        String parameter = e.getParameterName();
        log.error(ErrorResponse.ERROR_MISSING_REQUIRED_GET_PARAMETER_MESSAGE + " [{}]", parameter);
        SrhrErrorDto dto = SrhrErrorDto.of(
                ErrorResponse.ERROR_MISSING_REQUIRED_GET_PARAMETER_MESSAGE + " " + parameter, CODE_MISSING_PARAM, null);
        return buildErrorResponse(dto, HttpStatus.BAD_REQUEST);
    }

    // ------------------------------------------------------------------ error dto

    private SrhrErrorDto resolveError(String message, String traceId) {
        return isResentError(message)
                ? parseResentError(message, traceId)
                : SrhrErrorDto.of(message, "", traceId);
    }

    private static boolean isResentError(String message) {
        return message != null && message.contains(RESENT_ERROR_MARKER);
    }

    /** Достаёт из сообщения исключения JSON вида {"error": {...}}, который вернул нижестоящий сервис. */
    private SrhrErrorDto parseResentError(String message, String traceId) {
        try {
            String json = message.substring(message.indexOf(RESENT_ERROR_MARKER));
            JsonNode errorNode = objectMapper.readTree(json).path("error");
            SrhrError error = objectMapper
                    .readerFor(SrhrError.class)
                    .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(errorNode);

            if (error.getDetails() == null || error.getDetails().isEmpty()) {
                error.setDetails(new ArrayList<>(List.of(
                        new SrhrErrorDetail(errorNode.path("code").asText(""), ErrorResponse.DEFAULT_ERROR_MESSAGE))));
            }
            if (error.getLogId() == null || error.getLogId().isBlank()) {
                error.setLogId(traceId);
            }
            return new SrhrErrorDto(error);
        } catch (Exception e) {
            return SrhrErrorDto.of(e.getMessage(), CODE_HANDLER_EXCEPTION, traceId);
        }
    }

    private ResponseEntity<Object> buildErrorResponse(SrhrErrorDto dto, HttpStatusCode status) {
        String logMessage = "Не указано";
        try {
            logMessage = objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            log.error("Не удалось сериализовать ДТО ошибки: {}", e.getMessage());
        }
        log.error("Возникла ошибка при выполнении запроса: {}", LogUtil.getLogMsgWithLkrExtensions(logMessage));
        return new ResponseEntity<>(dto, status);
    }

    // ------------------------------------------------------------------ audit

    private record AuditContext(String requestType, String sessionId, LocalDateTime sessionCreatedAt, String traceId) {

        static AuditContext from(HttpServletRequest req) {
            Object requestType = req.getAttribute(ATTR_REQUEST_TYPE);
            Object sessionId = req.getAttribute(ATTR_SESSION_ID);
            Object sessionCreatedAt = req.getAttribute(ATTR_SESSION_CREATED_AT);
            Object traceId = req.getAttribute(ATTR_TRACE_ID);

            if (requestType == null || sessionId == null || sessionCreatedAt == null) {
                log.warn(ErrorResponse.ERROR_NOT_ALL_REQUIRED_AUDIT_PARAMS_MESSAGE);
            }
            return new AuditContext(
                    requestType != null ? requestType.toString() : UNDEFINED,
                    sessionId != null ? sessionId.toString() : UNDEFINED,
                    sessionCreatedAt != null ? (LocalDateTime) sessionCreatedAt : LocalDateTime.now(),
                    traceId != null ? traceId.toString() : UNDEFINED);
        }
    }

    @FunctionalInterface
    private interface AuditAction {
        void send(AuditSender sender, String sessionId, LocalDateTime sessionCreatedAt) throws Exception;
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

    /** @param errorText текст для лога, только для UNEXPECTED_ERROR; иначе null */
    private void sendAudit(AuditContext ctx, AuditEvent event, String errorText) {
        if (errorText != null) {
            log.warn("Неуспешное выполнение эндпоинта: [{}]", errorText);
        }
        AuditSender sender = auditSenderResolver.resolve(ctx.requestType());
        try {
            event.action.send(sender, ctx.sessionId(), ctx.sessionCreatedAt());
        } catch (Exception ex) {
            log.error("Ошибка отправки события [{}] в аудит: [{}]", event.code.apply(sender), ex.getMessage());
        }
    }
}
