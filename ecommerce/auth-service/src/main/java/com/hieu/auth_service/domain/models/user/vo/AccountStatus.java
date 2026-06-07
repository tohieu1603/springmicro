package com.hieu.auth_service.domain.models.user.vo;

import java.time.Instant;

/**
 * Value Object representing the security and lifecycle status of a User Account.
 * Implemented as a Java Record for native immutability and value-based equality.
 */
public record AccountStatus(
        boolean enabled,
        boolean accountNonExpired,
        boolean accountNonLocked,
        boolean credentialsNonExpired,
        Instant lastLogin
) {

    // ── Factory Methods ──

    public static AccountStatus createActive() {
        return new AccountStatus(true, true, true, true, null);
    }

    /**
     * Reconstitutes an account status from persistent storage.
     * Dùng hàm này để map dữ liệu từ Entity (DB) lên Domain Object.
     */
    public static AccountStatus of(
            boolean enabled,
            boolean accountNonExpired,
            boolean accountNonLocked,
            boolean credentialsNonExpired,
            Instant lastLogin
    ) {
        return new AccountStatus(enabled, accountNonExpired, accountNonLocked, credentialsNonExpired, lastLogin);
    }

    public static AccountStatus createDisabled() {
        return new AccountStatus(false, true, true, true, null);
    }

    public static AccountStatus createLocked() {
        return new AccountStatus(true, true, false, true, null);
    }

    // ── State Transition Methods (Immutability Enforced) ──

    public AccountStatus withLastLogin(Instant loginTime) {
        return new AccountStatus(
                this.enabled,
                this.accountNonExpired,
                this.accountNonLocked,
                this.credentialsNonExpired,
                loginTime
        );
    }

    public AccountStatus lock() {
        return new AccountStatus(
                this.enabled,
                this.accountNonExpired,
                false, // Update locked state
                this.credentialsNonExpired,
                this.lastLogin
        );
    }

    public AccountStatus unlock() {
        return new AccountStatus(
                this.enabled,
                this.accountNonExpired,
                true, // Update unlocked state
                this.credentialsNonExpired,
                this.lastLogin
        );
    }

    public AccountStatus disable() {
        return new AccountStatus(
                false, // Update enabled state
                this.accountNonExpired,
                this.accountNonLocked,
                this.credentialsNonExpired,
                this.lastLogin
        );
    }

    public AccountStatus enable() {
        return new AccountStatus(
                true, // Update enabled state
                this.accountNonExpired,
                this.accountNonLocked,
                this.credentialsNonExpired,
                this.lastLogin
        );
    }

    // ── Business Logic ──

    public boolean isActive() {
        return enabled && accountNonExpired && accountNonLocked && credentialsNonExpired;
    }
}