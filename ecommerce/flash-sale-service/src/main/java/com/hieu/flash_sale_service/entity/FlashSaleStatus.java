package com.hieu.flash_sale_service.entity;

/** Lifecycle states of a flash sale. Terminal states: ENDED, CANCELLED. */
public enum FlashSaleStatus {

    SCHEDULED {
        @Override
        public boolean canTransitionTo(FlashSaleStatus next) {
            return next == ACTIVE || next == CANCELLED;
        }
    },
    ACTIVE {
        @Override
        public boolean canTransitionTo(FlashSaleStatus next) {
            return next == ENDED || next == CANCELLED;
        }
    },
    ENDED {
        @Override
        public boolean canTransitionTo(FlashSaleStatus next) {
            return false;
        }
    },
    CANCELLED {
        @Override
        public boolean canTransitionTo(FlashSaleStatus next) {
            return false;
        }
    };

    /** Returns true if transitioning to {@code next} is a legal state machine move. */
    public abstract boolean canTransitionTo(FlashSaleStatus next);
}
