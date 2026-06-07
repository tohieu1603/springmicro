package com.hieu.shipping_service.dto;

import java.time.Instant;

/** Public tracking information — no sensitive address data. */
public record TrackingInfoDTO(
        String trackingNumber,
        String carrier,
        String status,
        String city,
        Instant estimatedDeliveryDate,
        Instant actualDeliveryDate
) {}
