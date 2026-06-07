package com.hieu.payment_service.exception;

public class DuplicatePaymentException extends RuntimeException {

    public DuplicatePaymentException(String orderId) {
        super("Payment already exists for order: " + orderId);
    }
}
