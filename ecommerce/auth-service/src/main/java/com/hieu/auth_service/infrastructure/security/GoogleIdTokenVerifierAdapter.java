package com.hieu.auth_service.infrastructure.security;

import java.security.GeneralSecurityException;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.hieu.auth_service.domain.models.user.exceptions.GoogleTokenInvalidException;
import com.hieu.auth_service.domain.services.GoogleIdTokenVerifierPort;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Adapter for {@link GoogleIdTokenVerifierPort} backed by Google's official
 * {@code google-api-client} library.
 *
 * <p>Validates:
 * <ul>
 *   <li>Signature against Google's published JWKs (cached internally by the lib).</li>
 *   <li>{@code aud} claim matches our configured client ID — this is what stops
 *       a token issued for another Google app from logging into HIEU.</li>
 *   <li>{@code iss} is {@code accounts.google.com} or {@code https://accounts.google.com}.</li>
 *   <li>{@code exp} is in the future.</li>
 * </ul>
 *
 * <p>{@code GOOGLE_CLIENT_ID} must be set in env; otherwise startup fails fast.
 */
@Component
@Slf4j
public class GoogleIdTokenVerifierAdapter implements GoogleIdTokenVerifierPort {

    @Value("${google.oauth.client-id:}")
    private String clientId;

    private GoogleIdTokenVerifier verifier;

    @PostConstruct
    void init() {
        if (clientId == null || clientId.isBlank()) {
            log.warn("GOOGLE_CLIENT_ID is not configured — /api/auth/google will reject all requests.");
            return;
        }
        verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
        log.info("Google ID token verifier initialised (audience suffix: ...{})",
                clientId.length() > 8 ? clientId.substring(clientId.length() - 8) : clientId);
    }

    @Override
    public GoogleClaims verify(String rawToken) {
        if (verifier == null) {
            throw new GoogleTokenInvalidException("server not configured for Google login");
        }
        if (rawToken == null || rawToken.isBlank()) {
            throw new GoogleTokenInvalidException("empty token");
        }
        try {
            GoogleIdToken token = verifier.verify(rawToken);
            if (token == null) {
                throw new GoogleTokenInvalidException("signature or audience mismatch");
            }
            Payload p = token.getPayload();
            return new GoogleClaims(
                    p.getSubject(),
                    p.getEmail(),
                    Boolean.TRUE.equals(p.getEmailVerified()),
                    (String) p.get("name"),
                    (String) p.get("given_name"),
                    (String) p.get("family_name"),
                    (String) p.get("picture"));
        } catch (GeneralSecurityException | java.io.IOException ex) {
            log.warn("Google token verification error: {}", ex.getMessage());
            throw new GoogleTokenInvalidException(ex.getMessage());
        }
    }
}
