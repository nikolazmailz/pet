package com.pet.requisition.infrastructure.json;

import com.fasterxml.jackson.databind.*;
import com.pet.requisition.application.model.*;
import com.pet.requisition.application.port.out.*;

public class JacksonMessageCodecAdapter implements MessageCodecPort {
    private final ObjectMapper mapper;

    public JacksonMessageCodecAdapter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public RequestEnvelope<RawPayload> deserializeRequest(String message) throws Exception {
        JsonNode root = mapper.readTree(message);
        JsonNode p = root.get("payload");
        RawPayload payload = (p == null || p.isNull()) ? null : new RawPayload(mapper.writeValueAsString(p));
        return new RequestEnvelope<>(text(root, "correlationId"), text(root, "requestType"), text(root, "sessionId"), text(root, "adLogin"), text(root, "traceId"), text(root, "channel"), text(root, "realm"), text(root, "fileUuid"), payload);
    }

    public <T> T deserializePayload(RawPayload payload, Class<T> targetClass) throws Exception {
        if (payload == null) return null;
        return mapper.readValue(payload.json(), targetClass);
    }

    public String serializeResponse(ResponseEnvelope<?> response) throws Exception {
        return mapper.writeValueAsString(response);
    }

    public String serializeObject(Object value) throws Exception {
        return mapper.writeValueAsString(value);
    }

    private String text(JsonNode root, String name) {
        JsonNode n = root.get(name);
        return n == null || n.isNull() ? null : n.asText();
    }
}
