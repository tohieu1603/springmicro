package com.hieu.order_service.domain.events.order;

import com.hieu.order_service.domain.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record OrderPaymentCompletedEvent(
        UUID eventId,
        Instant occurredOn,
        String orderId,
        String orderNumber,
        String userId
) implements DomainEvent {

    /** Convenience factory — generates eventId and occurredOn automatically. */
    public OrderPaymentCompletedEvent(String orderId, String orderNumber, String userId) {
        this(UUID.randomUUID(), Instant.now(), orderId, orderNumber, userId);
    }

    @Override
    public String aggregateId() { return orderId; }
}
