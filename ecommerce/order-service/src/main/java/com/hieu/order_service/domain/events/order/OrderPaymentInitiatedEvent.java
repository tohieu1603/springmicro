package com.hieu.order_service.domain.events.order;

import com.hieu.order_service.domain.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record OrderPaymentInitiatedEvent(
        UUID eventId,
        Instant occurredOn,
        String orderId,
        String orderNumber,
        String paymentId
) implements DomainEvent {

    /** Convenience factory — generates eventId and occurredOn automatically. */
    public OrderPaymentInitiatedEvent(String orderId, String orderNumber, String paymentId) {
        this(UUID.randomUUID(), Instant.now(), orderId, orderNumber, paymentId);
    }

    @Override
    public String aggregateId() { return orderId; }
}
