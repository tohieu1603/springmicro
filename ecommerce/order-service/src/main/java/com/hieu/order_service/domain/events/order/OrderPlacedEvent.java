package com.hieu.order_service.domain.events.order;

import com.hieu.order_service.domain.events.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Fired after an order is persisted with initial state. Consumers (analytics, search
 * signals, recommendation, shipping) need the full snapshot — items, total, and the
 * shipping address — so we carry them on the event instead of forcing each consumer
 * to round-trip back to order-service.
 */
public record OrderPlacedEvent(
        UUID eventId,
        Instant occurredOn,
        String orderId,
        String orderNumber,
        String userId,
        BigDecimal totalAmount,
        String paymentMethod,
        AddressSnapshot shippingAddress,
        List<ItemSnapshot> items
) implements DomainEvent {

    public OrderPlacedEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(occurredOn, "occurredOn");
        Objects.requireNonNull(orderId, "orderId");
        Objects.requireNonNull(orderNumber, "orderNumber");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(totalAmount, "totalAmount");
        items = List.copyOf(Objects.requireNonNullElse(items, List.of()));
    }

    /** Convenience factory — generates eventId and occurredOn automatically. */
    public OrderPlacedEvent(String orderId, String orderNumber, String userId,
                            BigDecimal totalAmount, String paymentMethod,
                            AddressSnapshot shippingAddress, List<ItemSnapshot> items) {
        this(UUID.randomUUID(), Instant.now(),
             orderId, orderNumber, userId, totalAmount, paymentMethod, shippingAddress, items);
    }

    @Override
    public String aggregateId() { return orderId; }

    /** Flat projection of the shipping address — avoids leaking the VO across services. */
    public record AddressSnapshot(String recipientName, String recipientPhone,
                                   String street, String ward, String district,
                                   String city, String country, String postalCode) {}

    /** Flat projection of an order item. */
    public record ItemSnapshot(String productId, String productName, String variantId,
                                String variantSku, BigDecimal unitPrice, int quantity) {}
}
