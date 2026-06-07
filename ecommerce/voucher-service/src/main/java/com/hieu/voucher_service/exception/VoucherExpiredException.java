package com.hieu.voucher_service.exception;

public class VoucherExpiredException extends RuntimeException {

    public VoucherExpiredException(String code) {
        super("Voucher is expired or not yet active: " + code);
    }
}
