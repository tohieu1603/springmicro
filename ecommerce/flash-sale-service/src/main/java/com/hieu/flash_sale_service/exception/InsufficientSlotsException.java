package com.hieu.flash_sale_service.exception;

/** Thrown when no slots remain for a flash sale. */
public class InsufficientSlotsException extends RuntimeException {

    public InsufficientSlotsException(String saleId) {
        super("No slots remaining for flash sale id=" + saleId);
    }

    public InsufficientSlotsException(String saleId, String detail) {
        super("Insufficient slots for flash sale id=" + saleId + ": " + detail);
    }
}
