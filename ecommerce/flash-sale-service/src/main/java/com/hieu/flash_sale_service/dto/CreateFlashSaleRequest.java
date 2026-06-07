package com.hieu.flash_sale_service.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

/** Request body for POST /api/flash-sales (ADMIN). */
public record CreateFlashSaleRequest(
        @NotBlank String productId,
        String productName,
        @NotNull @DecimalMin("0.01") BigDecimal originalPrice,
        @NotNull @DecimalMin("0.01") BigDecimal salePrice,
        @Min(1) int totalSlots,
        @Min(1) int maxPerUser,
        @NotNull @Future Instant startTime,
        @NotNull @Future Instant endTime,
        String description
) {
    public CreateFlashSaleRequest {
        if (maxPerUser <= 0) maxPerUser = 1;
    }
}
