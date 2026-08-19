package com.pet.requisition.application.port.out;

public interface RequestContextPort {
    void set(String adLogin, String sessionId, String channel);

    void clear();
}
