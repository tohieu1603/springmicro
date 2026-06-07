package com.hieu.catalog_service.application.command.variant;

import com.hieu.catalog_service.application.common.Command;

public record RemoveVariantCommand(String productId, String variantId, String deletedBy)
        implements Command<Void> {}
