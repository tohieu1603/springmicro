package com.hieu.auth_service.domain.models.user.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Password VO (unit)")
class PasswordTest {

    @Test
    @DisplayName("accepts a valid raw password and marks it as needing encoding")
    void validRaw() {
        Password p = Password.createRaw("password1");

        assertThat(p.needsEncoding()).isTrue();
        assertThat(p.encoded()).isFalse();
        assertThat(p.value()).isEqualTo("password1");
    }

    @Test
    @DisplayName("rejects raw passwords that are too short")
    void tooShort() {
        assertThatThrownBy(() -> Password.createRaw("pw1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rejects raw passwords with no digit")
    void noDigit() {
        assertThatThrownBy(() -> Password.createRaw("onlyletters"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rejects raw passwords with no letter")
    void noLetter() {
        assertThatThrownBy(() -> Password.createRaw("12345678"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rejects raw passwords over the max length")
    void tooLong() {
        String longPw = "a1" + "x".repeat(200);
        assertThatThrownBy(() -> Password.createRaw(longPw))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("encoded passwords skip raw-strength validation")
    void encodedSkipsValidation() {
        // "short" would fail raw rules, but as an already-encoded hash it is accepted.
        assertThatCode(() -> Password.createEncoded("short"))
                .doesNotThrowAnyException();
        assertThat(Password.createEncoded("short").needsEncoding()).isFalse();
    }

    @Test
    @DisplayName("rejects null/empty regardless of encoding flag")
    void rejectsEmpty() {
        assertThatThrownBy(() -> Password.createEncoded("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Password.createRaw(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("toString never leaks the password value")
    void toStringHidesValue() {
        assertThat(Password.createRaw("password1").toString()).doesNotContain("password1");
    }
}
