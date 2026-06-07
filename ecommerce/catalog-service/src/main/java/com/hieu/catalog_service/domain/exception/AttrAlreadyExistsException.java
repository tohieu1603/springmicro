package com.hieu.catalog_service.domain.exception;

import com.hieu.catalog_service.domain.shared.DomainException;
import com.hieu.common.error.ErrorCode;

public final class AttrAlreadyExistsException extends DomainException {
    public AttrAlreadyExistsException(String code) {
        super(ErrorCode.ATTR_ALREADY_EXISTS.code(), "Attribute code already exists: " + code);
    }
}
