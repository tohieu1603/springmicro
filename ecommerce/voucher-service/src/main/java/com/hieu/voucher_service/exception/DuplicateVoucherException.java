package com.hieu.voucher_service.exception;

public class DuplicateVoucherException extends RuntimeException {

    public DuplicateVoucherException(String code) {
        super("Voucher with code already exists: " + code);
    }
}
