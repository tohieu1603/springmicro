package com.hieu.order_service.domain.events.returnrequest;

import com.hieu.order_service.domain.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record OrderReturnRequestedEvent(
        UUID eventId,
        Instant occurredOn,
        String orderId,
        String returnRequestId,
        String userId,
        String reason
) implements DomainEvent {

    public OrderReturnRequestedEvent(String orderId, String returnRequestId, String userId, String reason) {
        this(UUID.randomUUID(), Instant.now(), orderId, returnRequestId, userId, reason);
    }

    @Override
    public String aggregateId() { return orderId; }
}
