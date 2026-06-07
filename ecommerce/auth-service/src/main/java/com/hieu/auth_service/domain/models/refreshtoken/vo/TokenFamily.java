package com.hieu.auth_service.domain.models.refreshtoken.vo;

import java.util.UUID;

/**
 * Value Object: family id shared by all tokens from the same login session.
 * <p>
 * When token reuse is detected every token in the family is revoked,
 * forcing the user to re-authenticate (Rotation + Family Revocation pattern)
 * <p>
 * The constructor validates UUID format so corrupted strings from the
 * persistence layer cannot masquerade as valid families
 */
public record TokenFamily(String value) {

    public TokenFamily {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("TokenFamily cannot be null or empty");
        }
        // Chuẩn hóa chuỗi trước khi gán và validate
        value = value.trim();
        UUID.fromString(value); // Sẽ throw IllegalArgumentException nếu sai format UUID
    }

    public static TokenFamily of(String value) {
        return new TokenFamily(value);
    }

    public static TokenFamily generate() {
        return new TokenFamily(UUID.randomUUID().toString());
    }

    public boolean isSameFamily(TokenFamily other) {
        // Sử dụng other.value() thay vì other.value đối với Record
        return other != null && this.value.equals(other.value());
    }
}