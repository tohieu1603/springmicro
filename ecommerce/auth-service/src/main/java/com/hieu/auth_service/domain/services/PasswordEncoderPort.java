package com.hieu.auth_service.domain.services;

/**
 * Outbound port for password hashing.
 *
 * <p>Infrastructure supplies the adapter (BCrypt, Argon2, ...). Keeping the port
 * in {@code domain.services} lets aggregates enforce credential invariants without
 * depending on Spring Security or any library.
 */
public interface PasswordEncoderPort {
    String encode(String rawPassword);
    boolean matches(String rawPassword, String encodedPassword);
}
