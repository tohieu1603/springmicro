package com.hieu.order_service.domain.exception;

import com.hieu.common.error.ErrorCode;
import com.hieu.order_service.domain.shared.DomainException;

public final class InvalidOrderStateException extends DomainException {
    public InvalidOrderStateException(String message) {
        super(ErrorCode.ORDER_INVALID_STATE.code(), message);
    }
}
