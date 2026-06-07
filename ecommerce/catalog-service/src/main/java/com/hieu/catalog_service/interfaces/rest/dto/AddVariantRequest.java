package com.hieu.catalog_service.interfaces.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

public record AddVariantRequest(
        @NotBlank String sku,
        @NotNull @PositiveOrZero BigDecimal price,
        BigDecimal cost,
        BigDecimal salePrice,
        String image,
        BigDecimal weight,
        @PositiveOrZero int quantity,
        @Valid List<CreateProductRequest.AttrRequest> attrs
) {}
