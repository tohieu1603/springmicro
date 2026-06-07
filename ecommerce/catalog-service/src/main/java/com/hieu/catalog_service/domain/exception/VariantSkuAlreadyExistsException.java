package com.hieu.catalog_service.domain.exception;

import com.hieu.catalog_service.domain.shared.DomainException;
import com.hieu.common.error.ErrorCode;

public final class VariantSkuAlreadyExistsException extends DomainException {
    public VariantSkuAlreadyExistsException(String sku) {
        super(ErrorCode.VARIANT_SKU_ALREADY_EXISTS.code(), "SKU already exists: " + sku);
    }
}
