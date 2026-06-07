package com.hieu.order_service.domain.events.order;

import com.hieu.order_service.domain.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record OrderShippedEvent(
        UUID eventId,
        Instant occurredOn,
        String orderId,
        String orderNumber,
        String userId,
        String shipmentId
) implements DomainEvent {

    /** Convenience factory — generates eventId and occurredOn automatically. */
    public OrderShippedEvent(String orderId, String orderNumber, String userId, String shipmentId) {
        this(UUID.randomUUID(), Instant.now(), orderId, orderNumber, userId, shipmentId);
    }

    @Override
    public String aggregateId() { return orderId; }
}
