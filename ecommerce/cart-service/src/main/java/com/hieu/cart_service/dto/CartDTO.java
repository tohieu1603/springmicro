package com.hieu.cart_service.dto;

import java.math.BigDecimal;
import java.util.List;

/** Full cart snapshot returned by REST endpoints. */
public record CartDTO(
        String userId,
        List<CartItemDTO> items,
        int totalItems,
        BigDecimal totalAmount,
        List<String> warnings
) {}
