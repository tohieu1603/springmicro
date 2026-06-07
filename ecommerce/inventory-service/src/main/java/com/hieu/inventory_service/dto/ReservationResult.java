package com.hieu.inventory_service.dto;

/** Result returned from reserve/confirm/release operations. */
public record ReservationResult(boolean success, String reservationId, String errorMessage) {

    public static ReservationResult success(String orderId) {
        return new ReservationResult(true, orderId, null);
    }

    public static ReservationResult failure(String message) {
        return new ReservationResult(false, null, message);
    }
}
