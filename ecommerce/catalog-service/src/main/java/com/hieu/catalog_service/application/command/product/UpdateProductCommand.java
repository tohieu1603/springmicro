package com.hieu.catalog_service.application.command.product;

import com.hieu.catalog_service.application.common.Command;
import com.hieu.catalog_service.application.dto.ProductDTO;

/** Partial update of core product fields. {@code null} fields are left unchanged. */
public record UpdateProductCommand(
        String productId,
        String name,
        String description,
        String categoryId,
        String brand,
        String updatedBy
) implements Command<ProductDTO> {}
