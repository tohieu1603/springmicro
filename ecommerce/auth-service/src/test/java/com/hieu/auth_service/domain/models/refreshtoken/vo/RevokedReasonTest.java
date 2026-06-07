package com.hieu.auth_service.domain.models.refreshtoken.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RevokedReason VO (unit)")
class RevokedReasonTest {

    @Test
    @DisplayName("REUSE_DETECTED and FAMILY_REVOKED are security-related; NORMAL is not")
    void securityRelated() {
        assertThat(RevokedReason.REUSE_DETECTED.isSecurityRelated()).isTrue();
        assertThat(RevokedReason.FAMILY_REVOKED.isSecurityRelated()).isTrue();
        assertThat(RevokedReason.NORMAL.isSecurityRelated()).isFalse();
        assertThat(RevokedReason.USER_INITIATED.isSecurityRelated()).isFalse();
    }

    @Test
    @DisplayName("isNormal() and shouldRevokeFamily() flag the right reasons")
    void predicates() {
        assertThat(RevokedReason.NORMAL.isNormal()).isTrue();
        assertThat(RevokedReason.FAMILY_REVOKED.shouldRevokeFamily()).isTrue();
        assertThat(RevokedReason.NORMAL.shouldRevokeFamily()).isFalse();
    }

    @Test
    @DisplayName("of() normalises case and maps to the canonical constants")
    void ofNormalises() {
        assertThat(RevokedReason.of("normal")).isEqualTo(RevokedReason.NORMAL);
        assertThat(RevokedReason.of("  reuse_detected ")).isEqualTo(RevokedReason.REUSE_DETECTED);
    }

    @Test
    @DisplayName("constructor uppercases the value and rejects blanks")
    void normalisationAndValidation() {
        assertThat(new RevokedReason("custom").value()).isEqualTo("CUSTOM");
        assertThatThrownBy(() -> new RevokedReason("  ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RevokedReason.of(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
