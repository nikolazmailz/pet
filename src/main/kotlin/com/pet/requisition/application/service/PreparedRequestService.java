package com.pet.requisition.application.service;

import com.pet.requisition.application.exception.*;
import com.pet.requisition.application.model.*;
import com.pet.requisition.application.port.out.*;
import java.util.*;

public class PreparedRequestService {
    public static final String STATUS_SENT = "SENT";
    private final MessageCodecPort codec;
    private final FileStoragePort fileStorage;
    private final RolePort rolePort;
    private final StaffPort staffPort;
    private final RequestPersistencePort persistence;
    private final RequestContextPort context;
    private final RequestLogPort log;

    public PreparedRequestService(MessageCodecPort codec, FileStoragePort fileStorage, RolePort rolePort, StaffPort staffPort, RequestPersistencePort persistence, RequestContextPort context, RequestLogPort log) {
        this.codec = codec;
        this.fileStorage = fileStorage;
        this.rolePort = rolePort;
        this.staffPort = staffPort;
        this.persistence = persistence;
        this.context = context;
        this.log = log;
    }

    public PreparedRequest<RawPayload> prepare(RequestEnvelope<RawPayload> envelope, String originalMessage) throws Exception {
        long startedAt = System.currentTimeMillis();
        SystemParams params = null;
        RequestRecord record = null;
        try {
            RequestEnvelope<RawPayload> restored = restoreFromFilestorageIfNeeded(envelope);
            params = createSystemParams(restored);
            context.set(params.getAdLogin(), params.getSessionId(), params.getChannel());
            log.incomingMessage(params);
            record = persistence.saveRequest(params.getCorrelationId(), null, params.getSessionId(), originalMessage, null, STATUS_SENT);
            enrichRoles(params);
            enrichTabNumber(params);
            return new PreparedRequest<>(restored, params, record, startedAt);
        } catch (Exception e) {
            String code = e instanceof ProcessingException pe ? pe.getCode() : "500";
            throw new PreparationException(code, e.getMessage(), e, params, record, startedAt);
        }
    }

    private RequestEnvelope<RawPayload> restoreFromFilestorageIfNeeded(RequestEnvelope<RawPayload> e) throws Exception {
        if (e.fileUuid() == null || e.fileUuid().isBlank()) return e;
        String stored = fileStorage.getMessage(e.adLogin(), e.sessionId(), e.traceId(), e.fileUuid());
        return codec.deserializeRequest(stored);
    }

    private SystemParams createSystemParams(RequestEnvelope<?> r) {
        SystemParams p = new SystemParams();
        p.setCorrelationId(r.correlationId());
        p.setRequestType(r.requestType());
        p.setSessionId(r.sessionId());
        p.setAdLogin(r.adLogin());
        p.setTraceId(r.traceId());
        p.setChannel(r.channel());
        p.setRealm(r.realm());
        return p;
    }

    private void enrichRoles(SystemParams p) throws Exception {
        List<String> roles = rolePort.getRoles(p.getAdLogin(), p.getChannel(), p.getSessionId(), p.getTraceId());
        if (roles == null || roles.isEmpty())
            throw new ProcessingException("404", "Роль для юзера " + p.getAdLogin() + " не найдена.");
        String role = roles.stream().max(Comparator.comparingInt(this::priority)).orElseThrow();
        p.setRole(role);
        p.setRolesHeader(codec.serializeObject(roles));
        persistence.updateRequestRole(p.getCorrelationId(), role);
    }

    private void enrichTabNumber(SystemParams p) {
        String tab = staffPort.getTabNumber(p.getAdLogin(), p.getSessionId(), p.getTraceId());
        if (tab == null || tab.isBlank())
            throw new ProcessingException("404", "Табельный номер юзера " + p.getAdLogin() + " не найден.");
        p.setTabNumber(tab);
        persistence.updateRequestTabNumber(p.getCorrelationId(), tab);
    }

    private int priority(String role) {
        return switch (role) {
            case "MSS_FULL" -> 400;
            case "MSS_STANDARD" -> 300;
            case "MSS_LIMITED" -> 200;
            case "ESS" -> 100;
            default -> 0;
        };
    }
}
