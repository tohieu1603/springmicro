package com.hieu.shipping_service.entity;

/** Shipment lifecycle states with strict transition rules. */
public enum ShipmentStatus {
    PENDING,
    PICKED_UP,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    FAILED,
    RETURNED;

    /** Returns true when transitioning from {@code this} → {@code next} is permitted. */
    public boolean canTransitionTo(ShipmentStatus next) {
        return switch (this) {
            case PENDING          -> next == PICKED_UP || next == FAILED || next == RETURNED;
            case PICKED_UP        -> next == IN_TRANSIT || next == FAILED;
            case IN_TRANSIT       -> next == OUT_FOR_DELIVERY || next == FAILED;
            case OUT_FOR_DELIVERY -> next == DELIVERED || next == FAILED;
            case DELIVERED        -> next == RETURNED;
            case FAILED, RETURNED -> false;
        };
    }
}
