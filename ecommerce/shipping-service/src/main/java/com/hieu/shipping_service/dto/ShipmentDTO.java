package com.hieu.shipping_service.dto;

import java.time.Instant;

/** Read-only shipment projection returned to clients. */
public record ShipmentDTO(
        String id,
        String orderId,
        String userId,
        String carrier,
        String trackingNumber,
        String status,
        String recipientName,
        String recipientPhone,
        String addressLine,
        String ward,
        String district,
        String city,
        String country,
        Instant estimatedDeliveryDate,
        Instant actualDeliveryDate,
        String notes,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {}
