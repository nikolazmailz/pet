package com.pet.requisition.infrastructure.kafka;

import org.springframework.kafka.core.KafkaTemplate;

public class ResponseKafkaProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;

    public ResponseKafkaProducer(KafkaTemplate<String, String> kafkaTemplate, String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void send(String message) {
        kafkaTemplate.send(topic, message).join();
    }
}
