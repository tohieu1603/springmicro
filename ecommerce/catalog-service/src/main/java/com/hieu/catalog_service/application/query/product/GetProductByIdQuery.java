package com.hieu.catalog_service.application.query.product;

import com.hieu.catalog_service.application.common.Query;
import com.hieu.catalog_service.application.dto.ProductDTO;

public record GetProductByIdQuery(String productId) implements Query<ProductDTO> {}
