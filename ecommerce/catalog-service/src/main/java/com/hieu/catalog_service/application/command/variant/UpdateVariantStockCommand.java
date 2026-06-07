package com.hieu.catalog_service.application.command.variant;

import com.hieu.catalog_service.application.common.Command;
import com.hieu.catalog_service.application.dto.VariantDTO;

/** Replace stock outright (idempotent). For deltas use {@link AdjustVariantStockCommand}. */
public record UpdateVariantStockCommand(
        String productId,
        String variantId,
        int quantity,
        String updatedBy
) implements Command<VariantDTO> {}
