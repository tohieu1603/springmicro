package com.hieu.auth_service.domain.models.user.vo;


/**
 * Value Object representing User Password.
 * Encapsulates password validation rules and encoding state.
 */
public record Password(String value, boolean encoded) {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 100;

    // ── Compact Constructor ──
    public Password {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        if (!encoded) {
            validateRawPassword(value);
        }
    }

    public static Password createRaw(String rawPassword) {
        return new Password(rawPassword, false);
    }

    public static Password createEncoded(String encodedPassword) {
        return new Password(encodedPassword, true);
    }

    // Chuyển thành static method vì chỉ là hàm tiện ích nội bộ
    private static void validateRawPassword(String password) {
        if (password.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_LENGTH + " characters long");
        }
        if (password.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Password must not exceed " + MAX_LENGTH + " characters");
        }
        if (!containsDigit(password)) {
            throw new IllegalArgumentException("Password must contain at least one digit");
        }
        if (!containsLetter(password)) {
            throw new IllegalArgumentException("Password must contain at least one letter");
        }
    }

    private static boolean containsDigit(String password) {
        return password.chars().anyMatch(Character::isDigit);
    }

    private static boolean containsLetter(String password) {
        return password.chars().anyMatch(Character::isLetter);
    }

    // Giữ nguyên tên để dùng tốt với class User cũ (newPassword.needsEncoding())
    public boolean needsEncoding() {
        return !encoded;
    }

    @Override
    public String toString() {
        return "Password{encoded=" + encoded + ", length=" + value.length() + "}";
    }
}