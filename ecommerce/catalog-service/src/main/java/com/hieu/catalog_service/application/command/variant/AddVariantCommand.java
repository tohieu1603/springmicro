package com.hieu.catalog_service.application.command.variant;

import com.hieu.catalog_service.application.command.product.CreateProductCommand.AttrCmd;
import com.hieu.catalog_service.application.common.Command;
import com.hieu.catalog_service.application.dto.VariantDTO;

import java.math.BigDecimal;
import java.util.List;

public record AddVariantCommand(
        String productId,
        String sku,
        BigDecimal price,
        BigDecimal cost,
        BigDecimal salePrice,
        String image,
        BigDecimal weight,
        int quantity,
        List<AttrCmd> attrs,
        String createdBy
) implements Command<VariantDTO> {}
