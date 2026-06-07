package com.hieu.payment_service.exception;

public class PaymentNotFoundException extends RuntimeException {

    private PaymentNotFoundException(String message, boolean unused) {
        super(message);
    }

    public PaymentNotFoundException(String id) {
        super("Payment not found: " + id);
    }

    public static PaymentNotFoundException forOrder(String orderId) {
        return new PaymentNotFoundException("Payment not found for order: " + orderId, true);
    }
}
