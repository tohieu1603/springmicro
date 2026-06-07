package com.hieu.flash_sale_service.dto;

/** Response for GET /api/flash-sales/{id}/availability. */
public record AvailabilityResponse(
        int totalSlots,
        int reservedSlots,
        int remainingSlots,
        boolean available,
        String reason
) {}
