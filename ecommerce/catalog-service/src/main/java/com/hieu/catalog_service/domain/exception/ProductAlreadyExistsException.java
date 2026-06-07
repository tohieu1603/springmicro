package com.hieu.catalog_service.domain.exception;

import com.hieu.catalog_service.domain.shared.DomainException;
import com.hieu.common.error.ErrorCode;

public final class ProductAlreadyExistsException extends DomainException {
    public ProductAlreadyExistsException(String slug) {
        super(ErrorCode.PRODUCT_ALREADY_EXISTS.code(), "Product slug already in use: " + slug);
    }
}
