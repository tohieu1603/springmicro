package com.hieu.auth_service.domain.models.user.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Username VO (unit)")
class UsernameTest {

    @Test
    @DisplayName("trims surrounding whitespace")
    void trims() {
        assertThat(Username.of("  alice_01 ").value()).isEqualTo("alice_01");
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "user-name", "user_name", "ABC123"})
    @DisplayName("accepts letters, digits, underscores and hyphens (3-50 chars)")
    void acceptsValid(String value) {
        assertThat(Username.of(value).value()).isEqualTo(value);
    }

    @Test
    @DisplayName("rejects usernames shorter than 3 characters")
    void tooShort() {
        assertThatThrownBy(() -> Username.of("ab")).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"with space", "bad!char", "emoji😀", "dot.name"})
    @DisplayName("rejects illegal characters")
    void rejectsIllegalChars(String value) {
        assertThatThrownBy(() -> Username.of(value)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rejects null/blank")
    void rejectsBlank() {
        assertThatThrownBy(() -> Username.of(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Username.of("   ")).isInstanceOf(IllegalArgumentException.class);
    }
}
