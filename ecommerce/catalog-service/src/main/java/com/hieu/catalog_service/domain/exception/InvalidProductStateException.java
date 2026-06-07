package com.hieu.catalog_service.domain.exception;

import com.hieu.catalog_service.domain.shared.DomainException;
import com.hieu.common.error.ErrorCode;

/** Thrown when a product operation violates a lifecycle invariant (e.g. editing a deleted product). */
public final class InvalidProductStateException extends DomainException {
    public InvalidProductStateException(String message) {
        super(ErrorCode.PRODUCT_INVALID_STATE.code(), message);
    }
}
