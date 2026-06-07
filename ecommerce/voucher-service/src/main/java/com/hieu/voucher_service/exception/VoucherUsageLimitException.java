package com.hieu.voucher_service.exception;

public class VoucherUsageLimitException extends RuntimeException {

    public VoucherUsageLimitException(String code) {
        super("Voucher usage limit exceeded for: " + code);
    }
}
