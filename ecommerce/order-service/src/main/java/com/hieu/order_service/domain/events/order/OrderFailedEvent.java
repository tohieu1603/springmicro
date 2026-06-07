package com.hieu.order_service.domain.events.order;

import com.hieu.order_service.domain.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record OrderFailedEvent(
        UUID eventId,
        Instant occurredOn,
        String orderId,
        String orderNumber,
        String userId,
        String reason
) implements DomainEvent {

    public OrderFailedEvent(String orderId, String orderNumber, String userId, String reason) {
        this(UUID.randomUUID(), Instant.now(), orderId, orderNumber, userId, reason);
    }

    @Override
    public String aggregateId() { return orderId; }
}
