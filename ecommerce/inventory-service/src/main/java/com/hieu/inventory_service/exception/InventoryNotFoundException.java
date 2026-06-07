package com.hieu.inventory_service.exception;

/** Thrown when an inventory record cannot be found. */
public class InventoryNotFoundException extends RuntimeException {

    public InventoryNotFoundException(String productId) {
        super("Inventory not found for productId: " + productId);
    }
}
