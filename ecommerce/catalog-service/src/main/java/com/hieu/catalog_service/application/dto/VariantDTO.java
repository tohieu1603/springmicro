package com.hieu.catalog_service.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record VariantDTO(
        String id,
        String productId,
        String sku,
        BigDecimal price,
        BigDecimal cost,
        BigDecimal salePrice,
        BigDecimal effectivePrice,
        String image,
        BigDecimal weight,
        int quantity,
        String status,
        boolean available,
        List<VariantAttrDTO> attrs
) {}
