package com.hieu.flash_sale_service.dto;

import com.hieu.flash_sale_service.entity.FlashSaleStatus;

import java.math.BigDecimal;
import java.time.Instant;

/** Read model for a flash sale. */
public record FlashSaleDTO(
        String id,
        String productId,
        String productName,
        BigDecimal originalPrice,
        BigDecimal salePrice,
        int totalSlots,
        int reservedSlots,
        int maxPerUser,
        Instant startTime,
        Instant endTime,
        FlashSaleStatus status,
        String description,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {}
