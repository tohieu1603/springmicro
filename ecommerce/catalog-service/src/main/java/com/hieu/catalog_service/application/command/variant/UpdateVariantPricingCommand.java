package com.hieu.catalog_service.application.command.variant;

import com.hieu.catalog_service.application.common.Command;
import com.hieu.catalog_service.application.dto.VariantDTO;

import java.math.BigDecimal;

public record UpdateVariantPricingCommand(
        String productId,
        String variantId,
        BigDecimal price,
        BigDecimal cost,
        BigDecimal salePrice,
        String updatedBy
) implements Command<VariantDTO> {}
