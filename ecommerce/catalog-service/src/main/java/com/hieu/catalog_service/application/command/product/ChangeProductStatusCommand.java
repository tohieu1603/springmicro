package com.hieu.catalog_service.application.command.product;

import com.hieu.catalog_service.application.common.Command;

public record ChangeProductStatusCommand(String productId, Transition transition, String updatedBy)
        implements Command<Void> {

    public enum Transition { ACTIVATE, DEACTIVATE, DRAFT }
}
