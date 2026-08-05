package ru.ntdev.srhr.common.audit.sender;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AuditSenderResolver {

    private final List<AuditSender> senders;

    public AuditSenderResolver(List<AuditSender> senders) {
        this.senders = senders;
    }

    public AuditSender resolve(String requestType) {
        return senders.stream()
                .filter(sender -> sender.supports(requestType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Не найден AuditSender для requestType: " + requestType));
    }
}
