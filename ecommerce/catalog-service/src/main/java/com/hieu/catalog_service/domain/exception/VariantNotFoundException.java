package com.hieu.catalog_service.domain.exception;

import com.hieu.catalog_service.domain.shared.DomainException;
import com.hieu.common.error.ErrorCode;

public final class VariantNotFoundException extends DomainException {
    public VariantNotFoundException(String variantId) {
        super(ErrorCode.VARIANT_NOT_FOUND.code(), "Variant not found: " + variantId);
    }

    public static VariantNotFoundException bySku(String sku) {
        return new VariantNotFoundException(sku, true);
    }

    private VariantNotFoundException(String sku, boolean bySku) {
        super(ErrorCode.VARIANT_NOT_FOUND.code(), "Variant not found by SKU: " + sku);
    }
}
