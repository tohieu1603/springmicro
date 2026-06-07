package com.hieu.catalog_service.domain.model.product.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Money VO (unit)")
class MoneyTest {

    @Test
    @DisplayName("scales to 2 decimals with HALF_UP rounding")
    void scaling() {
        assertThat(Money.of(new BigDecimal("10.005")).amount()).isEqualByComparingTo("10.01");
        assertThat(Money.of(new BigDecimal("10")).amount()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("rejects a negative amount and a null in the strict factory")
    void rejectsInvalid() {
        assertThatThrownBy(() -> Money.of(new BigDecimal("-1"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Money.of(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("ofNullable tolerates null")
    void ofNullable() {
        assertThat(Money.ofNullable(null)).isNull();
        assertThat(Money.ofNullable(new BigDecimal("5")).amount()).isEqualByComparingTo("5.00");
    }

    @Test
    @DisplayName("add and subtract within bounds")
    void arithmetic() {
        Money ten = Money.of(BigDecimal.TEN);
        assertThat(ten.add(Money.of(BigDecimal.valueOf(5))).amount()).isEqualByComparingTo("15.00");
        assertThat(ten.subtract(Money.of(BigDecimal.valueOf(4))).amount()).isEqualByComparingTo("6.00");
    }

    @Test
    @DisplayName("subtract that would go negative throws")
    void subtractNegative() {
        assertThatThrownBy(() -> Money.of(BigDecimal.valueOf(5)).subtract(Money.of(BigDecimal.TEN)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("comparison helpers")
    void comparisons() {
        Money five = Money.of(BigDecimal.valueOf(5));
        Money ten = Money.of(BigDecimal.TEN);
        assertThat(ten.isGreaterThan(five)).isTrue();
        assertThat(five.isLessThan(ten)).isTrue();
        assertThat(Money.zero().isZero()).isTrue();
    }
}
