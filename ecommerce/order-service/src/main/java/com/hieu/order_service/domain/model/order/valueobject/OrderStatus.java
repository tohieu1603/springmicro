package com.hieu.order_service.domain.model.order.valueobject;

import java.util.Set;

/** Order lifecycle state machine. */
public enum OrderStatus {

    PENDING {
        @Override public Set<OrderStatus> allowedTransitions() {
            return Set.of(INVENTORY_RESERVED, CANCELLED, FAILED);
        }
    },
    INVENTORY_RESERVED {
        @Override public Set<OrderStatus> allowedTransitions() {
            return Set.of(PAYMENT_PENDING, CANCELLED, FAILED);
        }
    },
    PAYMENT_PENDING {
        @Override public Set<OrderStatus> allowedTransitions() {
            return Set.of(PAYMENT_COMPLETED, PAYMENT_FAILED, CANCELLED);
        }
    },
    PAYMENT_COMPLETED {
        @Override public Set<OrderStatus> allowedTransitions() {
            return Set.of(CONFIRMED, FAILED);
        }
    },
    PAYMENT_FAILED {
        @Override public Set<OrderStatus> allowedTransitions() {
            return Set.of(CANCELLED);
        }
    },
    CONFIRMED {
        @Override public Set<OrderStatus> allowedTransitions() {
            return Set.of(SHIPPED, CANCELLED);
        }
    },
    SHIPPED {
        @Override public Set<OrderStatus> allowedTransitions() {
            return Set.of(DELIVERED, RETURN_REQUESTED);
        }
    },
    DELIVERED {
        @Override public Set<OrderStatus> allowedTransitions() {
            return Set.of(RETURN_REQUESTED);
        }
    },
    RETURN_REQUESTED {
        @Override public Set<OrderStatus> allowedTransitions() {
            return Set.of(RETURNED);
        }
    },
    CANCELLED {
        @Override public Set<OrderStatus> allowedTransitions() { return Set.of(); }
    },
    FAILED {
        @Override public Set<OrderStatus> allowedTransitions() { return Set.of(); }
    },
    RETURNED {
        @Override public Set<OrderStatus> allowedTransitions() { return Set.of(); }
    };

    public abstract Set<OrderStatus> allowedTransitions();

    public boolean canTransitionTo(OrderStatus next) {
        return allowedTransitions().contains(next);
    }
}
