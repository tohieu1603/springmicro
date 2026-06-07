package com.hieu.order_service.application.dto;

import java.math.BigDecimal;

public record OrderItemDTO(
        String id,
        String productId,
        String productName,
        String variantId,
        String variantSku,
        String variantImage,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal subtotal
) {}
