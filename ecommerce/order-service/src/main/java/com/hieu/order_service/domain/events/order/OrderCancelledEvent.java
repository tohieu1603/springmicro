package com.hieu.order_service.domain.events.order;

import com.hieu.order_service.domain.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record OrderCancelledEvent(
        UUID eventId,
        Instant occurredOn,
        String orderId,
        String orderNumber,
        String userId,
        String reason,
        String voucherCode
) implements DomainEvent {

    /** Convenience factory — generates eventId and occurredOn automatically. */
    public OrderCancelledEvent(String orderId, String orderNumber, String userId, String reason, String voucherCode) {
        this(UUID.randomUUID(), Instant.now(), orderId, orderNumber, userId, reason, voucherCode);
    }

    @Override
    public String aggregateId() { return orderId; }
}
