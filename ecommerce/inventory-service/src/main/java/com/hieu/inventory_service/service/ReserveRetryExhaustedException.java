package com.hieu.inventory_service.service;

/**
 * Marker thrown from {@code @Recover} so the outer catch block in {@code reserveStock}
 * runs (releases the Redis deduction). Returning a success/failure DTO from @Recover
 * marks the retry as "handled" and the exception never propagates — Redis leaks.
 */
public class ReserveRetryExhaustedException extends RuntimeException {
    private final String orderId;

    public ReserveRetryExhaustedException(String orderId, Throwable cause) {
        super("Reserve stock retries exhausted for orderId=" + orderId, cause);
        this.orderId = orderId;
    }

    public String getOrderId() {
        return orderId;
    }
}
