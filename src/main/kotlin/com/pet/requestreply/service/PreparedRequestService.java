package com.pet.requestreply.service;

import com.pet.requestreply.application.model.*;
import com.pet.requestreply.application.port.out.*;
import java.util.*;

public class PreparedRequestService {
    private final MessageCodecPort codec;
    private final FileStoragePort fileStorage;
    private final RolePort roles;
    private final StaffPort staff;
    private final RequestRepositoryPort repository;
    private final RequestContextPort context;
    private final RequestLogPort log;

    public PreparedRequestService(
            MessageCodecPort codec,
            FileStoragePort fileStorage,
            RolePort roles,
            StaffPort staff,
            RequestRepositoryPort repository,
            RequestContextPort context,
            RequestLogPort log
    ) {
        this.codec = codec;
        this.fileStorage = fileStorage;
        this.roles = roles;
        this.staff = staff;
        this.repository = repository;
        this.context = context;
        this.log = log;
    }

    public PreparedRequest<RawPayload> prepare(
            RequestEnvelope<RawPayload> envelope,
            String originalMessage
    ) throws Exception {
        long startedAt = System.currentTimeMillis();

        RequestEnvelope<RawPayload> restored = restoreFromFilestorageIfNeeded(envelope);
        SystemParams systemParams = createSystemParams(restored);

        context.set(
                systemParams.getAdLogin(),
                systemParams.getSessionId(),
                systemParams.getChannel()
        );

        log.messageReceived(systemParams);

        RequestRecord record = repository.save(
                systemParams.getCorrelationId(),
                systemParams.getSessionId(),
                originalMessage
        );

        enrichRoles(systemParams);
        enrichTabNumber(systemParams);

        return new PreparedRequest<>(restored, systemParams, record, startedAt);
    }

    private RequestEnvelope<RawPayload> restoreFromFilestorageIfNeeded(
            RequestEnvelope<RawPayload> envelope
    ) throws Exception {
        if (envelope.fileUuid() == null || envelope.fileUuid().isBlank()) {
            return envelope;
        }

        String stored = fileStorage.getMessage(
                envelope.adLogin(),
                envelope.sessionId(),
                envelope.traceId(),
                envelope.fileUuid()
        );

        return codec.deserializeRequest(stored);
    }

    private SystemParams createSystemParams(RequestEnvelope<?> request) {
        SystemParams params = new SystemParams();
        params.setCorrelationId(request.correlationId());
        params.setRequestType(request.requestType());
        params.setSessionId(request.sessionId());
        params.setAdLogin(request.adLogin());
        params.setTraceId(request.traceId());
        params.setChannel(request.channel());
        params.setRealm(request.realm());
        return params;
    }

    private void enrichRoles(SystemParams params) throws Exception {
        List<String> roleList = roles.getRoles(
                params.getAdLogin(),
                params.getChannel(),
                params.getSessionId(),
                params.getTraceId()
        );

        if (roleList == null || roleList.isEmpty()) {
            throw new IllegalStateException("Roles not found for " + params.getAdLogin());
        }

        String role = roleList.stream()
                .max(Comparator.comparingInt(this::priority))
                .orElseThrow();

        params.setRole(role);
        params.setRolesHeader(codec.serializeObject(roleList));
        repository.updateRole(params.getCorrelationId(), role);
    }

    private void enrichTabNumber(SystemParams params) {
        String tabNumber = staff.getTabNumber(
                params.getAdLogin(),
                params.getSessionId(),
                params.getTraceId()
        );

        if (tabNumber == null || tabNumber.isBlank()) {
            throw new IllegalStateException("Tab number not found for " + params.getAdLogin());
        }

        params.setTabNumber(tabNumber);
        repository.updateTabNumber(params.getCorrelationId(), tabNumber);
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
