package com.hieu.shipping_service.exception;

public class InvalidShipmentStateException extends RuntimeException {
    public InvalidShipmentStateException(String message) {
        super(message);
    }
}
