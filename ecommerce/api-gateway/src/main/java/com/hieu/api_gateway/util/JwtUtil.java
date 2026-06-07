package com.hieu.api_gateway.util;

import com.hieu.common.security.JwtTokenValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Thin Spring wrapper around {@link JwtTokenValidator}.
 *
 * <p>Reactive gateway code needs a framework-free helper to parse tokens during
 * filter execution. The shared validator is constructed once from the
 * {@code jwt.secret} property and reused.
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private JwtTokenValidator validator;

    /** Builds the validator once the secret has been injected. */
    @PostConstruct
    void init() {
        this.validator = new JwtTokenValidator(secret);
    }

    public boolean validateSignature(String token) { return validator.validateSignature(token); }
    public boolean isExpired(String token)          { return validator.isExpired(token); }

    public String extractUsername(String token)     { return validator.extractUsername(token); }
    public String extractUserId(String token)       { return validator.extractUserId(token); }
    public String extractTokenId(String token)      { return validator.extractTokenId(token); }
    public int    extractTokenVersion(String token) { return validator.extractTokenVersion(token); }
}
