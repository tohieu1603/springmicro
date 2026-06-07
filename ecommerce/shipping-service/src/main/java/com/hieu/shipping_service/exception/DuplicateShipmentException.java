package com.hieu.shipping_service.exception;

public class DuplicateShipmentException extends RuntimeException {
    public DuplicateShipmentException(String orderId) {
        super("Shipment already exists for orderId: " + orderId);
    }
}
