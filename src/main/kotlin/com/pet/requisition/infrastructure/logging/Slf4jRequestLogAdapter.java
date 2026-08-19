package com.pet.requisition.infrastructure.logging;
import org.slf4j.*;
import ru.ntdev.srhr.ms.requisition.application.model.SystemParams;
import ru.ntdev.srhr.ms.requisition.application.port.out.RequestLogPort;
public class Slf4jRequestLogAdapter implements RequestLogPort {
    private static final Logger log=LoggerFactory.getLogger(Slf4jRequestLogAdapter.class);
    public void incomingMessage(SystemParams p){log.info("Kafka message got requestType={}, correlationId={}, adLogin={}, sessionId={}, traceId={}",p.getRequestType(),p.getCorrelationId(),p.getAdLogin(),p.getSessionId(),p.getTraceId());}
    public void warning(String message,Throwable e){log.warn(message,e);}
}
