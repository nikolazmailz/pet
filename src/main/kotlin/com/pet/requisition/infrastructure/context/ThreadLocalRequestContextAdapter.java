package com.pet.requisition.infrastructure.context;

import com.pet.requisition.application.port.out.*;

public class ThreadLocalRequestContextAdapter implements RequestContextPort {
    private static final ThreadLocal<Context> HOLDER = new ThreadLocal<>();

    public void set(String adLogin, String sessionId, String channel) {
        HOLDER.set(new Context(adLogin, sessionId, channel));
    }

    public void clear() {
        HOLDER.remove();
    }

    public static Context current() {
        return HOLDER.get();
    }

    public record Context(String adLogin, String sessionId, String channel) {
    }
}
