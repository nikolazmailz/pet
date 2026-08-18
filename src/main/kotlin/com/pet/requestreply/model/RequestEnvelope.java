package com.pet.requestreply.model;

import java.io.*;
import java.util.*;

public record RequestEnvelope<T>(
        String correlationId,
        String requestType,
        String sessionId,
        String adLogin,
        String traceId,
        String channel,
        String realm,
        String fileUuid,
        T payload
) {
    public RequestEnvelope(T payload) {
        this(UUID.randomUUID().toString(), null, null, null, null, null, null, null, payload);
    }
}