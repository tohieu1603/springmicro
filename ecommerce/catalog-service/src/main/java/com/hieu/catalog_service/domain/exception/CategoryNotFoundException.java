package com.hieu.catalog_service.domain.exception;

import com.hieu.catalog_service.domain.shared.DomainException;
import com.hieu.common.error.ErrorCode;

public final class CategoryNotFoundException extends DomainException {
    public CategoryNotFoundException(String categoryId) {
        super(ErrorCode.CATEGORY_NOT_FOUND.code(), "Category not found: " + categoryId);
    }
}
