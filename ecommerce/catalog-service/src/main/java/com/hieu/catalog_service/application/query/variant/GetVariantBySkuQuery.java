package com.hieu.catalog_service.application.query.variant;

import com.hieu.catalog_service.application.common.Query;
import com.hieu.catalog_service.application.dto.VariantDTO;

public record GetVariantBySkuQuery(String sku) implements Query<VariantDTO> {}
