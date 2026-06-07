package com.hieu.inventory_service.service;

/**
 * Signals that a concurrent reservation with the same orderId already won the
 * unique-constraint race. Treated as idempotent success by reserveStock — Redis
 * was already deducted exactly once by the winner; no rollback needed.
 */
public class DuplicateReservationException extends RuntimeException {
    private final String orderId;

    public DuplicateReservationException(String orderId) {
        super("Reservation already exists for orderId=" + orderId);
        this.orderId = orderId;
    }

    public String getOrderId() {
        return orderId;
    }
}
