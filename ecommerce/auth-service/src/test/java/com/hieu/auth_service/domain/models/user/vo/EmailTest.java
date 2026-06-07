package com.hieu.auth_service.domain.models.user.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Email VO (unit)")
class EmailTest {

    @Test
    @DisplayName("normalises to trimmed lowercase and exposes domain/localPart")
    void normalisesAndSplits() {
        Email email = Email.of("  John.Doe@Example.COM ");

        assertThat(email.value()).isEqualTo("john.doe@example.com");
        assertThat(email.localPart()).isEqualTo("john.doe");
        assertThat(email.domain()).isEqualTo("example.com");
    }

    @Test
    @DisplayName("two emails differing only by case are equal")
    void caseInsensitiveEquality() {
        assertThat(Email.of("A@B.com")).isEqualTo(Email.of("a@b.com"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"no-at-sign", "missing@domain", "@nolocal.com", "spaces in@x.com"})
    @DisplayName("rejects malformed addresses")
    void rejectsMalformed(String bad) {
        assertThatThrownBy(() -> Email.of(bad)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rejects null/blank")
    void rejectsBlank() {
        assertThatThrownBy(() -> Email.of(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Email.of("   ")).isInstanceOf(IllegalArgumentException.class);
    }
}
