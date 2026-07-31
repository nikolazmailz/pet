package ru.ntdev.bscs.module.commons.audit.service.resolver;

import static ru.ntdev.bscs.module.commons.util.JSONUtil.gson;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;
import ru.ntdev.bscs.module.commons.audit.config.AuditResource;
import ru.ntdev.bscs.module.commons.util.JSONUtil;
import ru.vtb.omni.audit.lib.api.enums.AudLibEventClass;
import ru.vtb.omni.audit.lib.api.event.AuditMethodParams;
import ru.vtb.omni.audit.lib.api.resolver.AuditListFieldsResolver;

@RequiredArgsConstructor
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
@Slf4j
public class OperDescriptionResolver implements AuditListFieldsResolver {

  @Resource
  private AuditResource auditResource;

  public static final String CONSUMER = "consumser";
  public static final String SYSTEM_MODE_DTO = "systemModeDto";
  public static final String REQUEST_BODY = "requestBody";
  public static final String USER_SESSION_ID = "userSessionId";
  public static final String USER_SESSION_CREATED_AT = "userSessionCreatedAt";
  public static final String USER_SESSION_FINISHED_AT = "userSessionFinishedAt";
  public static final String SESSION_DURATION_MILLIS = "sessionDurationMillis";
  public static final String ERROR_MESSAGE = "errorMessage";
  public static final String TRACE_ID = "traceId";
  public static final String FINISHED_CAUSE = "finishedCause";
  public static final String OPER_DESCRIPTION = "oper_description";
  public static final String INITIATOR_CLIENT_APP_ID = "initiator_clientAppId";

  public Map<String, Object> getFields(String eventCode, AudLibEventClass audLibEventClass,
      AuditMethodParams auditMethodParams, Map<String, Object> currentFields) {
    Map<String, Object> params = new HashMap<>();

    if (auditMethodParams.getParams() != null
        && auditMethodParams.getParams().get(CONSUMER) != null) {
      params.put(CONSUMER, JSONUtil.gson.toJson(auditMethodParams.getParams().get(CONSUMER)));
    }
    if (auditMethodParams.getParams() != null
        && auditMethodParams.getParams().get(SYSTEM_MODE_DTO) != null) {
      params.put(SYSTEM_MODE_DTO,
          JSONUtil.gson.toJson(auditMethodParams.getParams().get(SYSTEM_MODE_DTO)));
    }

    if (auditMethodParams.getParams() != null) {
      if (auditMethodParams.getParams().get(REQUEST_BODY) != null) {
        params.put(REQUEST_BODY, gson.toJson(auditMethodParams.getParams().get(REQUEST_BODY)));
      }
    }
    if (auditResource.getStartSession() != null) {
      LocalDateTime finishedAt = LocalDateTime.now();
      params.put(USER_SESSION_CREATED_AT, auditResource.getStartSession());
      params.put(USER_SESSION_FINISHED_AT, finishedAt);
      params.put(SESSION_DURATION_MILLIS,
          ChronoUnit.MILLIS.between(auditResource.getStartSession(), finishedAt));
    }
    if (auditResource.getTraceId() != null) {
      params.put(TRACE_ID, auditResource.getTraceId());
    }

    if (auditResource.getErrorMessage() != null) {
      params.put(ERROR_MESSAGE, auditResource.getErrorMessage());
    }

    if (this.auditResource.getSessionId() != null) {
      params.put(USER_SESSION_ID, this.auditResource.getSessionId());
    } else {
      if (auditMethodParams.getParams() != null
          && auditMethodParams.getParams().get(USER_SESSION_ID) != null) {
        params.put(USER_SESSION_ID, auditMethodParams.getParams().get(USER_SESSION_ID));
      }
    }

    if (this.auditResource.getFinishedCause() != null) {
      params.put(FINISHED_CAUSE, this.auditResource.getFinishedCause());
    }

    Map<String, Object> result = new HashMap<>();
    result.put(OPER_DESCRIPTION, gson.toJson(params));
    if (this.auditResource.getInitiator() != null) {
      result.put(INITIATOR_CLIENT_APP_ID, this.auditResource.getInitiator());
    }
    return result;
  }

  public void setFinishedCause(int httpStatus) {
    this.auditResource.setFinishedCause("Завершение запроса со статусом: " + httpStatus);
  }

  public String getFieldName() {
    return OPER_DESCRIPTION;
  }
}
