package com.hieu.order_service.domain.exception;

import com.hieu.common.error.ErrorCode;
import com.hieu.order_service.domain.shared.DomainException;

public final class ReturnRequestNotFoundException extends DomainException {
    public ReturnRequestNotFoundException(Object id) {
        super(ErrorCode.RETURN_REQUEST_NOT_FOUND.code(), "ReturnRequest not found: " + id);
    }
}
