package com.hieu.shipping_service.exception;

public class ShipmentAccessDeniedException extends RuntimeException {
    public ShipmentAccessDeniedException(String id) {
        super("Access denied for shipment: " + id);
    }
}
