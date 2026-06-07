package com.hieu.catalog_service.interfaces.rest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record UpdateVariantPricingRequest(
        @NotNull @PositiveOrZero BigDecimal price,
        @PositiveOrZero BigDecimal cost,
        @PositiveOrZero BigDecimal salePrice
) {}
