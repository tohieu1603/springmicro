package com.hieu.order_service.infrastructure.persistence.mapper;

import com.hieu.order_service.domain.model.order.Order;
import com.hieu.order_service.domain.model.order.OrderItem;
import com.hieu.order_service.domain.model.order.valueobject.*;
import com.hieu.order_service.infrastructure.persistence.jpa.entities.OrderItemJpaEntity;
import com.hieu.order_service.infrastructure.persistence.jpa.entities.OrderJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Pure unit tests for the hand-written Order aggregate <-> JPA entity mapping. */
class OrderJpaMapperTest {

    private final OrderJpaMapper mapper = new OrderJpaMapper();

    private static final String USER = UUID.randomUUID().toString();

    private Order reconstituteOrder(String id, ReservationId reservationId) {
        var order = Order.reconstitute(
                OrderId.of(id),
                OrderNumber.of("ORD-20240101-000001"),
                UserId.of(USER),
                OrderStatus.CONFIRMED,
                Money.of(new BigDecimal("100.00")),
                Money.of(new BigDecimal("10.00")),
                Money.of(new BigDecimal("5.00")),
                Money.of(new BigDecimal("95.00")),
                "VC-10",
                RecipientName.of("Nguyen Van A"),
                RecipientPhone.of("0901234567"),
                ShippingAddress.of("12 Le Loi", "Ward 1", "District 1", "HCMC", "Vietnam", "70000"),
                "leave at door",
                "COD",
                "00000000-0000-0000-0000-000000000042",
                reservationId,
                "00000000-0000-0000-0000-000000000007",
                "idem-key-1",
                null,
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-02T00:00:00Z"),
                Instant.parse("2024-01-03T00:00:00Z"),
                Instant.parse("2024-01-04T00:00:00Z"),
                null,
                "creator",
                "updater",
                3L);
        order.addReconstitutedItem(OrderItem.reconstitute(
                "00000000-0000-0000-0000-000000000500", ProductId.of("00000000-0000-0000-0000-000000000900"), ProductName.of("Widget"),
                "00000000-0000-0000-0000-000000000901", "SKU-1", "img.png",
                Money.of(new BigDecimal("50.00")), Quantity.of(2)));
        return order;
    }

    @Test
    @DisplayName("toJpa copies every scalar, address and item field")
    void toJpa_mapsAllFields() {
        var order = reconstituteOrder("00000000-0000-0000-0000-000000000001", ReservationId.of("res-1"));

        var e = mapper.toJpa(order, null);

        assertThat(e.getId()).isEqualTo("00000000-0000-0000-0000-000000000001");
        assertThat(e.getOrderNumber()).isEqualTo("ORD-20240101-000001");
        assertThat(e.getUserId()).isEqualTo(USER);
        assertThat(e.getStatus()).isEqualTo("CONFIRMED");
        assertThat(e.getSubtotalAmount()).isEqualByComparingTo("100.00");
        assertThat(e.getDiscountAmount()).isEqualByComparingTo("10.00");
        assertThat(e.getShippingFee()).isEqualByComparingTo("5.00");
        assertThat(e.getTotalAmount()).isEqualByComparingTo("95.00");
        assertThat(e.getVoucherCode()).isEqualTo("VC-10");
        assertThat(e.getRecipientName()).isEqualTo("Nguyen Van A");
        assertThat(e.getRecipientPhone()).isEqualTo("0901234567");
        assertThat(e.getStreet()).isEqualTo("12 Le Loi");
        assertThat(e.getWard()).isEqualTo("Ward 1");
        assertThat(e.getDistrict()).isEqualTo("District 1");
        assertThat(e.getCity()).isEqualTo("HCMC");
        assertThat(e.getCountry()).isEqualTo("Vietnam");
        assertThat(e.getPostalCode()).isEqualTo("70000");
        assertThat(e.getNotes()).isEqualTo("leave at door");
        assertThat(e.getPaymentMethod()).isEqualTo("COD");
        assertThat(e.getPaymentId()).isEqualTo("00000000-0000-0000-0000-000000000042");
        assertThat(e.getReservationId()).isEqualTo("res-1");
        assertThat(e.getShipmentId()).isEqualTo("00000000-0000-0000-0000-000000000007");
        assertThat(e.getIdempotencyKey()).isEqualTo("idem-key-1");
        assertThat(e.getCreatedBy()).isEqualTo("creator");
        assertThat(e.getUpdatedBy()).isEqualTo("updater");
        assertThat(e.getVersion()).isEqualTo(3L);

        assertThat(e.getItems()).hasSize(1);
        var ie = e.getItems().get(0);
        assertThat(ie.getId()).isEqualTo("00000000-0000-0000-0000-000000000500");
        assertThat(ie.getOrder()).isSameAs(e);
        assertThat(ie.getProductId()).isEqualTo("00000000-0000-0000-0000-000000000900");
        assertThat(ie.getProductName()).isEqualTo("Widget");
        assertThat(ie.getVariantId()).isEqualTo("00000000-0000-0000-0000-000000000901");
        assertThat(ie.getVariantSku()).isEqualTo("SKU-1");
        assertThat(ie.getVariantImage()).isEqualTo("img.png");
        assertThat(ie.getUnitPrice()).isEqualByComparingTo("50.00");
        assertThat(ie.getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("toJpa maps null reservationId to null column")
    void toJpa_nullReservation() {
        var order = reconstituteOrder("00000000-0000-0000-0000-000000000001", null);

        var e = mapper.toJpa(order, null);

        assertThat(e.getReservationId()).isNull();
    }

    @Test
    @DisplayName("toJpa reuses the existing entity instance and clears its prior items")
    void toJpa_reusesExistingEntityAndReplacesItems() {
        var existing = new OrderJpaEntity();
        var stale = new OrderItemJpaEntity();
        stale.setProductId("00000000-0000-0000-0000-000000000111");
        existing.getItems().add(stale);

        var order = reconstituteOrder("00000000-0000-0000-0000-000000000001", ReservationId.of("res-1"));

        var e = mapper.toJpa(order, existing);

        assertThat(e).isSameAs(existing);
        assertThat(e.getItems()).hasSize(1);
        assertThat(e.getItems().get(0).getProductId()).isEqualTo("00000000-0000-0000-0000-000000000900");
    }

    @Test
    @DisplayName("toDomain rebuilds the aggregate with all value objects and items")
    void toDomain_mapsAllFields() {
        var e = mapper.toJpa(reconstituteOrder("00000000-0000-0000-0000-000000000001", ReservationId.of("res-9")), null);

        var domain = mapper.toDomain(e);

        assertThat(domain.getId().value()).isEqualTo("00000000-0000-0000-0000-000000000001");
        assertThat(domain.getOrderNumber().value()).isEqualTo("ORD-20240101-000001");
        assertThat(domain.getUserId().value()).isEqualTo(USER);
        assertThat(domain.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(domain.getTotalAmount().amount()).isEqualByComparingTo("95.00");
        assertThat(domain.getReservationId().value()).isEqualTo("res-9");
        assertThat(domain.getShipmentId()).isEqualTo("00000000-0000-0000-0000-000000000007");
        assertThat(domain.getVersion()).isEqualTo(3L);
        assertThat(domain.getItems()).hasSize(1);
        assertThat(domain.getItems().get(0).getProductName().value()).isEqualTo("Widget");
        assertThat(domain.getItems().get(0).getQuantity().value()).isEqualTo(2);
    }

    @Test
    @DisplayName("toDomain tolerates a null reservation column")
    void toDomain_nullReservation() {
        var e = mapper.toJpa(reconstituteOrder("00000000-0000-0000-0000-000000000001", null), null);

        var domain = mapper.toDomain(e);

        assertThat(domain.getReservationId()).isNull();
    }

    @Test
    @DisplayName("toJpa then toDomain preserves the aggregate (round trip)")
    void roundTrip() {
        var original = reconstituteOrder("00000000-0000-0000-0000-000000000099", ReservationId.of("res-rt"));

        var back = mapper.toDomain(mapper.toJpa(original, null));

        assertThat(back.getId().value()).isEqualTo("00000000-0000-0000-0000-000000000099");
        assertThat(back.getOrderNumber().value()).isEqualTo(original.getOrderNumber().value());
        assertThat(back.getRecipientPhone().value()).isEqualTo(original.getRecipientPhone().value());
        assertThat(back.getShippingAddress()).isEqualTo(original.getShippingAddress());
        assertThat(back.getItems().get(0).getId()).isEqualTo("00000000-0000-0000-0000-000000000500");
    }

    @Test
    @DisplayName("syncGeneratedIds copies DB ids back onto the aggregate and its items")
    void syncGeneratedIds_assignsIds() {
        // a freshly created order has null ids
        var order = Order.create(
                UserId.of(USER), OrderNumber.of("ORD-20240101-000002"),
                RecipientName.of("B"), RecipientPhone.of("0901234567"),
                ShippingAddress.of("s", "w", "d", "c", null, null),
                "COD", null, null, null, "creator");
        order.addItem(OrderItem.create(
                ProductId.of("00000000-0000-0000-0000-000000000001"), ProductName.of("P"), "00000000-0000-0000-0000-000000000002", "sku", "img",
                Money.of(new BigDecimal("10.00")), Quantity.of(1)));
        assertThat(order.getId()).isNull();
        assertThat(order.getItems().get(0).getId()).isNull();

        var saved = new OrderJpaEntity();
        saved.setId("00000000-0000-0000-0000-000000000777");
        var savedItem = new OrderItemJpaEntity();
        savedItem.setId("00000000-0000-0000-0000-000000000888");
        saved.getItems().add(savedItem);

        mapper.syncGeneratedIds(order, saved);

        assertThat(order.getId().value()).isEqualTo("00000000-0000-0000-0000-000000000777");
        assertThat(order.getItems().get(0).getId()).isEqualTo("00000000-0000-0000-0000-000000000888");
    }

    @Test
    @DisplayName("syncGeneratedIds only syncs up to the smaller item-list size")
    void syncGeneratedIds_mismatchedSizes() {
        var order = Order.create(
                UserId.of(USER), OrderNumber.of("ORD-20240101-000003"),
                RecipientName.of("B"), RecipientPhone.of("0901234567"),
                ShippingAddress.of("s", "w", "d", "c", null, null),
                "COD", null, null, null, "creator");
        order.addItem(OrderItem.create(
                ProductId.of("00000000-0000-0000-0000-000000000001"), ProductName.of("P"), "00000000-0000-0000-0000-000000000002", "sku", "img",
                Money.of(new BigDecimal("10.00")), Quantity.of(1)));

        var saved = new OrderJpaEntity();
        saved.setId("00000000-0000-0000-0000-000000000001");
        // saved has zero items -> loop body never runs, no exception
        mapper.syncGeneratedIds(order, saved);

        assertThat(order.getId().value()).isEqualTo("00000000-0000-0000-0000-000000000001");
        assertThat(order.getItems().get(0).getId()).isNull();
    }
}
