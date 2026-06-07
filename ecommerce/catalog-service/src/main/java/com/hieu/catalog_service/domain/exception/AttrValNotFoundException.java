package com.hieu.catalog_service.domain.exception;

import com.hieu.catalog_service.domain.shared.DomainException;
import com.hieu.common.error.ErrorCode;

public final class AttrValNotFoundException extends DomainException {
    public AttrValNotFoundException(String valId) {
        super(ErrorCode.ATTR_VAL_NOT_FOUND.code(), "Attribute value not found: " + valId);
    }
}
