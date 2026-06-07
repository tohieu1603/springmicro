package com.hieu.auth_service.domain.models.refreshtoken.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TokenFamily VO (unit)")
class TokenFamilyTest {

    @Test
    @DisplayName("generate() produces a valid UUID-backed family")
    void generate() {
        TokenFamily family = TokenFamily.generate();
        assertThat(UUID.fromString(family.value())).isNotNull();
    }

    @Test
    @DisplayName("isSameFamily() compares the underlying value")
    void isSameFamily() {
        String id = UUID.randomUUID().toString();
        assertThat(TokenFamily.of(id).isSameFamily(TokenFamily.of(id))).isTrue();
        assertThat(TokenFamily.of(id).isSameFamily(TokenFamily.generate())).isFalse();
        assertThat(TokenFamily.of(id).isSameFamily(null)).isFalse();
    }

    @Test
    @DisplayName("rejects non-UUID and blank values")
    void rejectsInvalid() {
        assertThatThrownBy(() -> TokenFamily.of("not-a-uuid")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TokenFamily.of("  ")).isInstanceOf(IllegalArgumentException.class);
    }
}
