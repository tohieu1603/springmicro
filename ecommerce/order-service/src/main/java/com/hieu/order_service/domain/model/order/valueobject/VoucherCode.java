package com.hieu.order_service.domain.model.order.valueobject;

import java.util.Objects;

public record VoucherCode(String value) {

    public VoucherCode {
        Objects.requireNonNull(value, "VoucherCode value");
        if (value.isBlank()) throw new IllegalArgumentException("VoucherCode must not be blank");
        value = value.trim().toUpperCase();
    }

    /** Strict factory — throws on null/blank. */
    public static VoucherCode of(String value) {
        return new VoucherCode(value);
    }

    /** Lenient factory — returns null for null/blank input (for optional voucher fields). */
    public static VoucherCode ofNullable(String value) {
        return (value == null || value.isBlank()) ? null : new VoucherCode(value);
    }
}
