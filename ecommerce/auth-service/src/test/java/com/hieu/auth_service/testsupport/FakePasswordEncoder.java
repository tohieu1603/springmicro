package com.hieu.auth_service.testsupport;

import com.hieu.auth_service.domain.services.PasswordEncoderPort;

/**
 * Deterministic {@link PasswordEncoderPort} for pure unit tests — no BCrypt, no Spring.
 *
 * <p>{@code encode("pw")} → {@code "ENC(pw)"} and {@code matches(raw, encoded)} simply checks
 * {@code encoded.equals(encode(raw))}. This makes credential assertions exact and fast while
 * still exercising the real domain code paths that depend on the encoder port.
 */
public final class FakePasswordEncoder implements PasswordEncoderPort {

    @Override
    public String encode(String rawPassword) {
        return "ENC(" + rawPassword + ")";
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return encodedPassword != null && encodedPassword.equals(encode(rawPassword));
    }
}
