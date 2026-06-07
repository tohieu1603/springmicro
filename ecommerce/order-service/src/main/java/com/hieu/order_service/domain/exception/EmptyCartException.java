package com.hieu.order_service.domain.exception;

import com.hieu.common.error.ErrorCode;
import com.hieu.order_service.domain.shared.DomainException;

public final class EmptyCartException extends DomainException {
    public EmptyCartException(String userId) {
        super(ErrorCode.ORDER_EMPTY_CART.code(), "Cart is empty for user: " + userId);
    }
}
