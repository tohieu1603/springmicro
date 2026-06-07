package com.hieu.order_service.domain.exception;

import com.hieu.common.error.ErrorCode;
import com.hieu.order_service.domain.shared.DomainException;

public final class DuplicateOrderException extends DomainException {
    public DuplicateOrderException(String key) {
        super(ErrorCode.ORDER_DUPLICATE.code(), "Duplicate order in progress for key: " + key);
    }
}
