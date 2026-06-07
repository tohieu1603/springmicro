package com.hieu.auth_service.domain.models.refreshtoken.vo;

/**
 * Value Object representing why a refresh token was revoked.
 * <p>
 * Sealed set of known reasons with factory method that enforces normalization.
 * Security-related reasons (REUSE_DETECTED, FAMILY_REVOKED) trigger stricter flows.
 */
public record RevokedReason(String value) {

    public static final RevokedReason NORMAL         = new RevokedReason("NORMAL");
    public static final RevokedReason REUSE_DETECTED = new RevokedReason("REUSE_DETECTED");
    public static final RevokedReason FAMILY_REVOKED = new RevokedReason("FAMILY_REVOKED");
    public static final RevokedReason USER_INITIATED = new RevokedReason("USER_INITIATED");
    public static final RevokedReason EXPIRED        = new RevokedReason("EXPIRED");

    public RevokedReason {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("RevokeReason value cannot be null or empty");
        }
        // Chuẩn hóa tham số đầu vào trước khi gán ngầm định vào Record field
        value = value.trim().toUpperCase();
    }

    public static RevokedReason of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("RevokeReason value cannot be null or empty");
        }
        return switch (value.trim().toUpperCase()) {
            case "NORMAL"         -> NORMAL;
            case "REUSE_DETECTED" -> REUSE_DETECTED;
            case "FAMILY_REVOKED" -> FAMILY_REVOKED;
            case "USER_INITIATED" -> USER_INITIATED;
            case "EXPIRED"        -> EXPIRED;
            default               -> new RevokedReason(value);
        };
    }

    /**
     * Whether this reason indicates a potential security attack
     */
    public boolean isSecurityRelated() {
        return this.equals(REUSE_DETECTED) || this.equals(FAMILY_REVOKED);
    }

    public boolean isNormal() {
        return this.equals(NORMAL);
    }

    /**
     * A reuse-detected revocation must cascade to the entire token family
     */
    public boolean shouldRevokeFamily() {
        return this.equals(FAMILY_REVOKED);
    }
}