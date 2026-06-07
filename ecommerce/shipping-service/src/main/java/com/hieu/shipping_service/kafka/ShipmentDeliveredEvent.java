package com.hieu.shipping_service.kafka;

import java.time.Instant;
import java.util.UUID;

/**
 * Integration event emitted when a shipment is marked delivered.
 * Published to {@code shipping.delivered}.
 */
public record ShipmentDeliveredEvent(
        String eventId,
        Instant occurredOn,
        String shipmentId,
        String orderId,
        String userId,
        Instant actualDeliveryDate
) {
    public static ShipmentDeliveredEvent of(String shipmentId, String orderId, String userId,
                                             Instant actualDeliveryDate) {
        return new ShipmentDeliveredEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                shipmentId, orderId, userId, actualDeliveryDate);
    }
}
