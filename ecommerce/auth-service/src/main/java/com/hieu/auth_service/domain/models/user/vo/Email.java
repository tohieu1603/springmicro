package com.hieu.auth_service.domain.models.user.vo;

import java.util.regex.Pattern;

/**
 * Value Object representing an Email Address.
 * Implemented as a record to ensure immutability and format validation.
 */
public record Email(String value) {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    // ── Compact Constructor (Tự động gán dữ liệu sau khi chạy xong logic này) ──
    public Email {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }

        // Chuẩn hóa dữ liệu ngay trên tham số truyền vào
        value = value.trim().toLowerCase();

        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }
    }

    public static Email of(String value) {
        return new Email(value);
    }

    // Đổi getDomain -> domain cho chuẩn style Record
    public String domain() {
        return value.substring(value.indexOf('@') + 1);
    }

    // Đổi getLocalPart -> localPart
    public String localPart() {
        return value.substring(0, value.indexOf('@'));
    }

    @Override
    public String toString() {
        return value;
    }
}