package com.hieu.flash_sale_service.dto;

import jakarta.validation.constraints.Min;

/** Request body for POST /api/flash-sales/{id}/participate. */
public record ParticipateRequest(@Min(1) int quantity) {
    public ParticipateRequest {
        if (quantity < 1) quantity = 1;
    }
}
