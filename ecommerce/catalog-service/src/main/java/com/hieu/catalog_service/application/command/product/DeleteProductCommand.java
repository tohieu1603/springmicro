package com.hieu.catalog_service.application.command.product;

import com.hieu.catalog_service.application.common.Command;

/** Soft-delete — flips status to {@code DELETED} and fires {@code ProductDeletedEvent}. */
public record DeleteProductCommand(String productId, String deletedBy) implements Command<Void> {}
