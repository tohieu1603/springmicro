package com.hieu.order_service.domain.exception;

import com.hieu.common.error.ErrorCode;
import com.hieu.order_service.domain.shared.DomainException;

public final class ServiceUnavailableException extends DomainException {
    public ServiceUnavailableException(String message) {
        super(ErrorCode.ORDER_SERVICE_UNAVAILABLE.code(), message);
    }
}
