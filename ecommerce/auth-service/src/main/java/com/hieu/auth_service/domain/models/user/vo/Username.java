package com.hieu.auth_service.domain.models.user.vo;


import java.util.regex.Pattern;

/**
 * Value Object representing a Username.
 * Ensures username format and length validation.
 */
public record Username(String value) {

    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 50;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

    // ── Compact Constructor ──
    public Username {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        // Tự động gán lại sau khi trim
        value = value.trim();

        if (value.length() < MIN_LENGTH) {
            throw new IllegalArgumentException(
                    "Username must be at least " + MIN_LENGTH + " characters long"
            );
        }

        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Username must not exceed " + MAX_LENGTH + " characters"
            );
        }

        if (!USERNAME_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Username can only contain letters, numbers, underscores and hyphens"
            );
        }
    }

    public static Username of(String value) {
        return new Username(value);
    }

    @Override
    public String toString() {
        return value;
    }
}