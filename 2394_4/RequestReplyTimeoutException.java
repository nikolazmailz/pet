package ru.ntdev.srhr.reqreply.exception;

import lombok.Getter;

import java.time.Duration;

/**
 * Ответ не появился за отведённое время.
 * Отличается от «ответ пришёл, но пустой» — прежний код возвращал {@code null} в обоих случаях.
 */
@Getter
public class RequestReplyTimeoutException extends RequestReplyException {

    private final String correlationId;
    private final Duration timeout;

    public RequestReplyTimeoutException(String correlationId, Duration timeout) {
        super("Ответ не получен за %s, correlationId=%s".formatted(timeout, correlationId));
        this.correlationId = correlationId;
        this.timeout = timeout;
    }
}
