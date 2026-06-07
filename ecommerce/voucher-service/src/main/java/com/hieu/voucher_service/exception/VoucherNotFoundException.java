package com.hieu.voucher_service.exception;

public class VoucherNotFoundException extends RuntimeException {

    public VoucherNotFoundException(Long id) {
        super("Voucher not found with id: " + id);
    }

    public VoucherNotFoundException(String code) {
        super("Voucher not found with code: " + code);
    }
}
