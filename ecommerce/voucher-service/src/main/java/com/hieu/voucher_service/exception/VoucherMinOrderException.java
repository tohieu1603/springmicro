package com.hieu.voucher_service.exception;

import java.math.BigDecimal;

public class VoucherMinOrderException extends RuntimeException {

    public VoucherMinOrderException(String code, BigDecimal minOrderAmount) {
        super("Order amount does not meet minimum requirement of " + minOrderAmount + " for voucher: " + code);
    }
}
