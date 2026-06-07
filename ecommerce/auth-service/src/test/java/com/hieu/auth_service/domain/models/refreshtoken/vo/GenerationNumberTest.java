package com.hieu.auth_service.domain.models.refreshtoken.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GenerationNumber VO (unit)")
class GenerationNumberTest {

    @Test
    @DisplayName("root() is 0 and reports isRoot()")
    void root() {
        GenerationNumber root = GenerationNumber.root();
        assertThat(root.value()).isZero();
        assertThat(root.isRoot()).isTrue();
    }

    @Test
    @DisplayName("next() increments by one and is no longer root")
    void next() {
        GenerationNumber g1 = GenerationNumber.root().next();
        assertThat(g1.value()).isEqualTo(1);
        assertThat(g1.isRoot()).isFalse();
    }

    @Test
    @DisplayName("comparison helpers describe chain position")
    void comparisons() {
        GenerationNumber g0 = GenerationNumber.of(0);
        GenerationNumber g3 = GenerationNumber.of(3);

        assertThat(g3.isAfter(g0)).isTrue();
        assertThat(g0.isBefore(g3)).isTrue();
        assertThat(g3.isSameAs(GenerationNumber.of(3))).isTrue();
        assertThat(g3.difference(g0)).isEqualTo(3);
    }

    @Test
    @DisplayName("rejects negative generations")
    void rejectsNegative() {
        assertThatThrownBy(() -> GenerationNumber.of(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
