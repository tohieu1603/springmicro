package com.hieu.auth_service.domain.models.role.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RoleName VO (unit)")
class RoleNameTest {

    @Test
    @DisplayName("adds the ROLE_ prefix and uppercases when missing")
    void addsPrefix() {
        assertThat(RoleName.of("admin").value()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("keeps an existing ROLE_ prefix")
    void keepsPrefix() {
        assertThat(RoleName.of("ROLE_USER").value()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("admin()/user() factory constants and getSimpleName()")
    void factoriesAndSimpleName() {
        assertThat(RoleName.admin().value()).isEqualTo("ROLE_ADMIN");
        assertThat(RoleName.user().value()).isEqualTo("ROLE_USER");
        assertThat(RoleName.admin().getSimpleName()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("rejects null/blank")
    void rejectsBlank() {
        assertThatThrownBy(() -> RoleName.of(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RoleName.of("  ")).isInstanceOf(IllegalArgumentException.class);
    }
}
