package com.hieu.catalog_service.application.query.variant;

import com.hieu.catalog_service.application.common.Query;
import com.hieu.catalog_service.application.dto.VariantDTO;

/**
 * Returns the {@link VariantDTO} for the requested SKU.
 * Callers compute {@code available = quantity >= requested} themselves to avoid a second fetch.
 */
public record CheckStockQuery(String sku, int requested) implements Query<VariantDTO> {}
