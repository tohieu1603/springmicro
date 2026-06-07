package com.hieu.flash_sale_service.exception;

/** Thrown when a flash sale with the given id cannot be found. */
public class FlashSaleNotFoundException extends RuntimeException {

    public FlashSaleNotFoundException(String id) {
        super("Flash sale not found: id=" + id);
    }
}
