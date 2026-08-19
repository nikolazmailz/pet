package com.pet.requisition.infrastructure.logging;

import com.pet.requisition.application.model.*;
import com.pet.requisition.application.port.out.*;
import org.slf4j.*;

public class Slf4jRequestLogAdapter implements RequestLogPort {
    private static final Logger log = LoggerFactory.getLogger(Slf4jRequestLogAdapter.class);

    public void incomingMessage(SystemParams p) {
        log.info("Kafka message got requestType={}, uuid={}, adLogin={}, sessionId={}, traceId={}", p.getRequestType(), p.getCorrelationId(), p.getAdLogin(), p.getSessionId(), p.getTraceId());
    }

    public void warning(String message, Throwable e) {
        log.warn(message, e);
    }
}
