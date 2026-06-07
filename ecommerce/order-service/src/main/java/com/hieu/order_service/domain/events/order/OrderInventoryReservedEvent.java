package com.hieu.order_service.domain.events.order;

import com.hieu.order_service.domain.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record OrderInventoryReservedEvent(
        UUID eventId,
        Instant occurredOn,
        String orderId,
        String orderNumber,
        String reservationId
) implements DomainEvent {

    /** Convenience factory — generates eventId and occurredOn automatically. */
    public OrderInventoryReservedEvent(String orderId, String orderNumber, String reservationId) {
        this(UUID.randomUUID(), Instant.now(), orderId, orderNumber, reservationId);
    }

    @Override
    public String aggregateId() { return orderId; }
}
