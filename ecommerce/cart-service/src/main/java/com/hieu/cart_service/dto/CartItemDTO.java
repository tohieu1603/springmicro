package com.hieu.cart_service.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Immutable view of a single cart line item. */
public record CartItemDTO(
        String id,
        String productId,
        String productName,
        String variantId,
        String variantSku,
        String variantImage,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal,
        String warning,
        Instant updatedAt
) {}
