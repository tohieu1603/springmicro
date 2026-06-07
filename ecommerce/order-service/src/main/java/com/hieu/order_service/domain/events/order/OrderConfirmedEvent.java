package com.hieu.order_service.domain.events.order;

import com.hieu.order_service.domain.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record OrderConfirmedEvent(
        UUID eventId,
        Instant occurredOn,
        String orderId,
        String orderNumber,
        String userId,
        String paymentId
) implements DomainEvent {

    /** Convenience factory — generates eventId and occurredOn automatically. */
    public OrderConfirmedEvent(String orderId, String orderNumber, String userId, String paymentId) {
        this(UUID.randomUUID(), Instant.now(), orderId, orderNumber, userId, paymentId);
    }

    @Override
    public String aggregateId() { return orderId; }
}
