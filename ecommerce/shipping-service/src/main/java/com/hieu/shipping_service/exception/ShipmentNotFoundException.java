package com.hieu.shipping_service.exception;

public class ShipmentNotFoundException extends RuntimeException {
    public ShipmentNotFoundException(String detail) {
        super("Shipment not found: " + detail);
    }
}
