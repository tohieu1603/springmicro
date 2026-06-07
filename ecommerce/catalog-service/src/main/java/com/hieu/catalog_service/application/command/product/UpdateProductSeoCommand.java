package com.hieu.catalog_service.application.command.product;

import com.hieu.catalog_service.application.common.Command;
import com.hieu.catalog_service.application.dto.ProductDTO;

public record UpdateProductSeoCommand(
        String productId,
        String metaTitle,
        String metaDescription,
        String metaKeywords,
        String updatedBy
) implements Command<ProductDTO> {}
