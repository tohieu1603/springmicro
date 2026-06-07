package com.hieu.voucher_service.exception;

public class VoucherInactiveException extends RuntimeException {

    public VoucherInactiveException(String code) {
        super("Voucher is inactive: " + code);
    }
}
