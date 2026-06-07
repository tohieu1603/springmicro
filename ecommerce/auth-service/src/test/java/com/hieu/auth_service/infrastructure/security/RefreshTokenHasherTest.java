package com.hieu.auth_service.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RefreshTokenHasher}: deterministic SHA-256 hex output, collision-free
 * mapping for distinct inputs, and the null/blank guard.
 */
class RefreshTokenHasherTest {

    private final RefreshTokenHasher hasher = new RefreshTokenHasher();

    @Test
    void hash_isDeterministicLowercase64CharHex() {
        String digest = hasher.hash("a-refresh-token-value");

        assertThat(digest).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(hasher.hash("a-refresh-token-value")).isEqualTo(digest); // stable across calls
    }

    @Test
    void hash_matchesKnownSha256Vector() {
        // FIPS 180-4 worked example: SHA-256("abc").
        assertThat(hasher.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void hash_distinctInputsProduceDistinctDigests() {
        assertThat(hasher.hash("token-one")).isNotEqualTo(hasher.hash("token-two"));
    }

    @Test
    void hash_nullOrBlankRejected() {
        assertThatThrownBy(() -> hasher.hash(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> hasher.hash("   ")).isInstanceOf(IllegalArgumentException.class);
    }
}
