package com.hieu.auth_service.domain.models.permission.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PermissionName VO (unit)")
class PermissionNameTest {

    @Test
    @DisplayName("of() uppercases resource/action and builds RESOURCE_ACTION value")
    void ofBuildsValue() {
        PermissionName name = PermissionName.of("user", "read");

        assertThat(name.resource()).isEqualTo("USER");
        assertThat(name.action()).isEqualTo("READ");
        assertThat(name.value()).isEqualTo("USER_READ");
        assertThat(name.toString()).isEqualTo("USER_READ");
    }

    @Test
    @DisplayName("isForResource()/allowsAction() are case-insensitive")
    void matching() {
        PermissionName name = PermissionName.of("USER", "READ");

        assertThat(name.isForResource("user")).isTrue();
        assertThat(name.allowsAction("read")).isTrue();
        assertThat(name.isForResource("order")).isFalse();
        assertThat(name.allowsAction("write")).isFalse();
    }

    @Test
    @DisplayName("fromString() parses a RESOURCE_ACTION string")
    void fromString() {
        PermissionName name = PermissionName.fromString("product_update");

        assertThat(name.resource()).isEqualTo("PRODUCT");
        assertThat(name.action()).isEqualTo("UPDATE");
    }

    @Test
    @DisplayName("rejects malformed strings and empty parts")
    void rejectsInvalid() {
        assertThatThrownBy(() -> PermissionName.fromString("noseparator"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PermissionName.fromString("a_b_c"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PermissionName.of("", "read"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
