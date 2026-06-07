package com.hieu.auth_service.domain.models.user.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PersonName VO (unit)")
class PersonNameTest {

    @Test
    @DisplayName("trims parts and builds the full name")
    void fullName() {
        PersonName name = PersonName.of("  Alice ", " Nguyen ");

        assertThat(name.firstName()).isEqualTo("Alice");
        assertThat(name.lastName()).isEqualTo("Nguyen");
        assertThat(name.fullName()).isEqualTo("Alice Nguyen");
    }

    @Test
    @DisplayName("rejects blank first or last name")
    void rejectsBlank() {
        assertThatThrownBy(() -> PersonName.of("", "Nguyen")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PersonName.of("Alice", null)).isInstanceOf(IllegalArgumentException.class);
    }
}
