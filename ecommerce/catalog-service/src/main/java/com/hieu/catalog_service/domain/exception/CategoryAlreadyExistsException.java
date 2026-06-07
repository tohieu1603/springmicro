package com.hieu.catalog_service.domain.exception;

import com.hieu.catalog_service.domain.shared.DomainException;
import com.hieu.common.error.ErrorCode;

public final class CategoryAlreadyExistsException extends DomainException {
    public CategoryAlreadyExistsException(String name) {
        super(ErrorCode.CATEGORY_ALREADY_EXISTS.code(), "Category name already exists: " + name);
    }
}
