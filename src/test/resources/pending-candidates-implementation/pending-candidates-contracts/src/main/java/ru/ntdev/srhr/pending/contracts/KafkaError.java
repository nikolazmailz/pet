package ru.ntdev.srhr.pending.contracts;

public record KafkaError(String code, String message, String traceId) {}
