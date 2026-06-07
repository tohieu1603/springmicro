package com.hieu.catalog_service.application.common;

import com.hieu.common.error.ErrorCode;

/** Thrown when the authenticated caller lacks permission for a command. */
public final class UnauthorizedException extends ApplicationException {
    public UnauthorizedException(String message) {
        super(ErrorCode.APP_FORBIDDEN.code(), message);
    }
}
