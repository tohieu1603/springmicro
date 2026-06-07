package com.hieu.catalog_service.domain.model.product.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Quantity VO (unit)")
class QuantityTest {

    @Test
    @DisplayName("rejects negative quantities")
    void rejectsNegative() {
        assertThatThrownBy(() -> Quantity.of(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("add increases and subtract decreases")
    void arithmetic() {
        assertThat(Quantity.of(5).add(3).value()).isEqualTo(8);
        assertThat(Quantity.of(5).subtract(2).value()).isEqualTo(3);
    }

    @Test
    @DisplayName("subtract below zero throws")
    void subtractInsufficient() {
        assertThatThrownBy(() -> Quantity.of(2).subtract(5)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("isLowStock only when positive and at/below threshold")
    void lowStock() {
        assertThat(Quantity.of(3).isLowStock(5)).isTrue();
        assertThat(Quantity.of(5).isLowStock(5)).isTrue();
        assertThat(Quantity.of(6).isLowStock(5)).isFalse();
        assertThat(Quantity.zero().isLowStock(5)).isFalse(); // zero is "out", not "low"
    }
}
