package com.pet.requisition.infrastructure.kafka;

import com.pet.requisition.application.port.out.*;
import com.pet.requisition.application.service.*;
import org.slf4j.*;

public class RequisitionKafkaListener {
    private static final Logger log = LoggerFactory.getLogger(RequisitionKafkaListener.class);
    private final MessageProcessingService processing;
    private final ResponseKafkaProducer producer;
    private final RequestContextPort context;

    public RequisitionKafkaListener(MessageProcessingService processing, ResponseKafkaProducer producer, RequestContextPort context) {
        this.processing = processing;
        this.producer = producer;
        this.context = context;
    }

    @KafkaListener(topics = "${app.kafka.request-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(String data, Acknowledgment ack) {
        try {
            String response = processing.processMessage(data);
            producer.send(response);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Fatal error: message cannot be converted to response", e);
            ack.acknowledge();
        } finally {
            context.clear();
        }
    }
}
