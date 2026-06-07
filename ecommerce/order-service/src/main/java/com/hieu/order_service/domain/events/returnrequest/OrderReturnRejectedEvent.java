package com.hieu.order_service.domain.events.returnrequest;

import com.hieu.order_service.domain.events.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record OrderReturnRejectedEvent(
        UUID eventId,
        Instant occurredOn,
        String orderId,
        String returnRequestId,
        String userId
) implements DomainEvent {

    public OrderReturnRejectedEvent(String orderId, String returnRequestId, String userId) {
        this(UUID.randomUUID(), Instant.now(), orderId, returnRequestId, userId);
    }

    @Override
    public String aggregateId() { return orderId; }
}
