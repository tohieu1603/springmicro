package com.hieu.catalog_service.domain.exception;

import com.hieu.catalog_service.domain.shared.DomainException;
import com.hieu.common.error.ErrorCode;

public final class ProductNotFoundException extends DomainException {
    public ProductNotFoundException(String criterion) {
        super(ErrorCode.PRODUCT_NOT_FOUND.code(), "Product not found: " + criterion);
    }
}
