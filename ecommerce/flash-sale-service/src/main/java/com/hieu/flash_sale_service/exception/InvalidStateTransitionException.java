package com.hieu.flash_sale_service.exception;

import com.hieu.flash_sale_service.entity.FlashSaleStatus;

/** Thrown when an illegal state transition is attempted on a flash sale. */
public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(String saleId, FlashSaleStatus from, FlashSaleStatus to) {
        super("Cannot transition flash sale id=" + saleId + " from " + from + " to " + to);
    }
}
