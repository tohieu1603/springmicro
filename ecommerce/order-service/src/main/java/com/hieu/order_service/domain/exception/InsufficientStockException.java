package com.hieu.order_service.domain.exception;

import com.hieu.common.error.ErrorCode;
import com.hieu.order_service.domain.shared.DomainException;

public final class InsufficientStockException extends DomainException {
    public InsufficientStockException(String message) {
        super(ErrorCode.ORDER_INSUFFICIENT_STOCK.code(), message);
    }
}
