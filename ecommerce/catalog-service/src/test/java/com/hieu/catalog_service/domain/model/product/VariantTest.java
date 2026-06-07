package com.hieu.catalog_service.domain.model.product;

import com.hieu.catalog_service.domain.model.attribute.valueobject.AttrId;
import com.hieu.catalog_service.domain.model.product.valueobject.Money;
import com.hieu.catalog_service.domain.model.product.valueobject.Quantity;
import com.hieu.catalog_service.domain.model.product.valueobject.Sku;
import com.hieu.catalog_service.domain.model.product.valueobject.VariantStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Variant aggregate-member (unit)")
class VariantTest {

    private static Variant variant(int qty, BigDecimal price, BigDecimal salePrice) {
        return Variant.create(
                Sku.of("SKU-1"),
                Money.of(price),
                null,
                Money.ofNullable(salePrice),
                null, null,
                Quantity.of(qty));
    }

    @Test
    @DisplayName("new variant with stock is ACTIVE and available; zero stock is OUT_OF_STOCK")
    void initialStatus() {
        assertThat(variant(5, BigDecimal.valueOf(100), null).getStatus()).isEqualTo(VariantStatus.ACTIVE);
        assertThat(variant(5, BigDecimal.valueOf(100), null).isAvailable()).isTrue();

        Variant empty = variant(0, BigDecimal.valueOf(100), null);
        assertThat(empty.getStatus()).isEqualTo(VariantStatus.OUT_OF_STOCK);
        assertThat(empty.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("stock reaching zero flips ACTIVE→OUT_OF_STOCK and back when replenished")
    void stockReconciliation() {
        Variant v = variant(5, BigDecimal.valueOf(100), null);

        v.updateStock(Quantity.zero());
        assertThat(v.getStatus()).isEqualTo(VariantStatus.OUT_OF_STOCK);

        v.updateStock(Quantity.of(10));
        assertThat(v.getStatus()).isEqualTo(VariantStatus.ACTIVE);
    }

    @Test
    @DisplayName("INACTIVE is a merchandising decision — stock changes never promote it back")
    void inactiveStaysInactive() {
        Variant v = variant(5, BigDecimal.valueOf(100), null);
        v.deactivate();

        v.adjustStock(10); // more stock, but still a deliberate INACTIVE

        assertThat(v.getStatus()).isEqualTo(VariantStatus.INACTIVE);
        assertThat(v.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("adjustStock below zero throws")
    void adjustBelowZero() {
        Variant v = variant(3, BigDecimal.valueOf(100), null);
        assertThatThrownBy(() -> v.adjustStock(-5)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("effective price prefers a cheaper sale price")
    void effectivePrice() {
        assertThat(variant(5, BigDecimal.valueOf(100), BigDecimal.valueOf(80)).getEffectivePrice().amount())
                .isEqualByComparingTo("80.00");
        assertThat(variant(5, BigDecimal.valueOf(100), null).getEffectivePrice().amount())
                .isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("a sale price above the regular price is rejected")
    void invalidSalePrice() {
        assertThatThrownBy(() -> variant(5, BigDecimal.valueOf(100), BigDecimal.valueOf(150)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("a duplicate attribute id is rejected")
    void duplicateAttr() {
        Variant v = variant(5, BigDecimal.valueOf(100), null);
        v.addAttr(VariantAttr.create(AttrId.of("1"), "COLOR", "Color", null, "Red"));

        assertThatThrownBy(() ->
                v.addAttr(VariantAttr.create(AttrId.of("1"), "COLOR", "Color", null, "Blue")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(v.getAttrs()).hasSize(1);
    }
}
