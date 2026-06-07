package com.hieu.auth_service.domain.models.user.vo;

/**
 * Value Object wrapping Google's {@code sub} claim — the immutable, opaque
 * identifier Google assigns to a user account.
 *
 * <p>Prefer this over email for account linking: email may change on Workspace
 * domains (e.g. when an org switches primary domain), but {@code sub} stays
 * the same for the entire lifetime of the Google account.
 */
public record GoogleSub(String value) {

    public GoogleSub {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("GoogleSub cannot be null or empty");
        }
        value = value.trim();
        // Google subs are numeric strings up to 255 chars; we just bound length
        // here and trust the verifier to assert authenticity.
        if (value.length() > 255) {
            throw new IllegalArgumentException("GoogleSub too long: " + value.length());
        }
    }

    public static GoogleSub of(String value) {
        return new GoogleSub(value);
    }

    @Override public String toString() { return value; }
}
