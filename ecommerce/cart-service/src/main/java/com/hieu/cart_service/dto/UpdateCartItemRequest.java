package com.hieu.cart_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Request body for PUT /api/cart/items/{variantId}. quantity=0 means delete. */
public record UpdateCartItemRequest(
        @NotNull @Min(0) @Max(999) Integer quantity
) {}
