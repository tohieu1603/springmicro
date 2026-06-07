package com.hieu.order_service.domain.exception;

import com.hieu.common.error.ErrorCode;
import com.hieu.order_service.domain.shared.DomainException;

public final class OrderNotFoundException extends DomainException {
    public OrderNotFoundException(Object id) {
        super(ErrorCode.ORDER_NOT_FOUND.code(), "Order not found: " + id);
    }
}
