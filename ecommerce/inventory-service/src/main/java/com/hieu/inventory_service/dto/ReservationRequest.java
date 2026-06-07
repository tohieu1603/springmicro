package com.hieu.inventory_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Request to reserve stock for an order. */
public record ReservationRequest(
    @NotBlank String orderId,
    @NotEmpty List<ReservationItem> items
) {
    /** Single line item in a reservation request. */
    public record ReservationItem(String productId, int quantity) {}
}
