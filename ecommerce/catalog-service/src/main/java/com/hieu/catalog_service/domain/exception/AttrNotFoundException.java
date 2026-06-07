package com.hieu.catalog_service.domain.exception;

import com.hieu.catalog_service.domain.shared.DomainException;
import com.hieu.common.error.ErrorCode;

public final class AttrNotFoundException extends DomainException {
    public AttrNotFoundException(String attrIdOrCode) {
        super(ErrorCode.ATTR_NOT_FOUND.code(), "Attribute not found: " + attrIdOrCode);
    }
}
