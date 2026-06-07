package com.hieu.order_service.domain.model.order;

import com.hieu.order_service.domain.events.order.*;
import com.hieu.order_service.domain.exception.InvalidOrderStateException;
import com.hieu.order_service.domain.model.order.valueobject.*;
import com.hieu.order_service.domain.shared.AggregateRoot;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Order aggregate root — rich domain model with 18+ fields. */
@Getter
public class Order extends AggregateRoot {

    private OrderId id;
    private OrderNumber orderNumber;
    private UserId userId;
    private OrderStatus status;
    private final List<OrderItem> items = new ArrayList<>();
    private Money subtotalAmount;
    private Money discountAmount;
    private Money shippingFee;
    private Money totalAmount;
    private String voucherCode;
    private RecipientName recipientName;
    private RecipientPhone recipientPhone;
    private ShippingAddress shippingAddress;
    private String notes;
    private String paymentMethod;
    private String paymentId;
    private ReservationId reservationId;
    private String shipmentId;
    private String idempotencyKey;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;
    private Instant deliveredAt;
    private Instant cancelledAt;
    private String createdBy;
    private String updatedBy;
    private Long version;

    private Order() {}

    /** Factory — new order, no id yet. */
    public static Order create(UserId userId, OrderNumber orderNumber,
                               RecipientName recipientName, RecipientPhone recipientPhone,
                               ShippingAddress shippingAddress, String paymentMethod,
                               String notes, String voucherCode, String idempotencyKey,
                               String createdBy) {
        var o = new Order();
        o.userId = userId;
        o.orderNumber = orderNumber;
        o.status = OrderStatus.PENDING;
        o.recipientName = recipientName;
        o.recipientPhone = recipientPhone;
        o.shippingAddress = shippingAddress;
        o.paymentMethod = paymentMethod;
        o.notes = notes;
        o.voucherCode = voucherCode;
        o.idempotencyKey = idempotencyKey;
        o.subtotalAmount = Money.ZERO;
        o.discountAmount = Money.ZERO;
        o.shippingFee = Money.ZERO;
        o.totalAmount = Money.ZERO;
        o.createdAt = Instant.now();
        o.updatedAt = Instant.now();
        o.createdBy = createdBy;
        o.updatedBy = createdBy;
        return o;
    }

    /** Factory — rehydrate from persistence. */
    public static Order reconstitute(OrderId id, OrderNumber orderNumber, UserId userId,
                                     OrderStatus status, Money subtotalAmount, Money discountAmount,
                                     Money shippingFee, Money totalAmount, String voucherCode,
                                     RecipientName recipientName, RecipientPhone recipientPhone,
                                     ShippingAddress shippingAddress, String notes,
                                     String paymentMethod, String paymentId,
                                     ReservationId reservationId, String shipmentId,
                                     String idempotencyKey, String failureReason,
                                     Instant createdAt, Instant updatedAt, Instant completedAt,
                                     Instant deliveredAt, Instant cancelledAt,
                                     String createdBy, String updatedBy, Long version) {
        var o = new Order();
        o.id = id;
        o.orderNumber = orderNumber;
        o.userId = userId;
        o.status = status;
        o.subtotalAmount = subtotalAmount;
        o.discountAmount = discountAmount;
        o.shippingFee = shippingFee;
        o.totalAmount = totalAmount;
        o.voucherCode = voucherCode;
        o.recipientName = recipientName;
        o.recipientPhone = recipientPhone;
        o.shippingAddress = shippingAddress;
        o.notes = notes;
        o.paymentMethod = paymentMethod;
        o.paymentId = paymentId;
        o.reservationId = reservationId;
        o.shipmentId = shipmentId;
        o.idempotencyKey = idempotencyKey;
        o.failureReason = failureReason;
        o.createdAt = createdAt;
        o.updatedAt = updatedAt;
        o.completedAt = completedAt;
        o.deliveredAt = deliveredAt;
        o.cancelledAt = cancelledAt;
        o.createdBy = createdBy;
        o.updatedBy = updatedBy;
        o.version = version;
        return o;
    }

    public void addItem(OrderItem item) {
        items.add(item);
        recalculate();
    }

    private void recalculate() {
        subtotalAmount = items.stream()
                .map(OrderItem::subtotal)
                .reduce(Money.ZERO, Money::add);
        totalAmount = subtotalAmount.subtract(discountAmount).add(shippingFee);
    }

    public void applyDiscount(Money discount) {
        this.discountAmount = discount;
        recalculate();
        updatedAt = Instant.now();
    }

    public void applyShippingFee(Money fee) {
        this.shippingFee = fee;
        recalculate();
        updatedAt = Instant.now();
    }

    /**
     * Called AFTER persistence assigns the DB id. Builds a rich snapshot (items + total +
     * shipping address) so downstream consumers (analytics/search/recommendation) get
     * everything they need without a follow-up round-trip to order-service.
     */
    public void raiseCreatedEvent() {
        var addressSnapshot = new OrderPlacedEvent.AddressSnapshot(
            recipientName.value(), recipientPhone.value(),
            shippingAddress.street(), shippingAddress.ward(), shippingAddress.district(),
            shippingAddress.city(), shippingAddress.country(), shippingAddress.postalCode());
        var itemSnapshots = items.stream()
            .map(i -> new OrderPlacedEvent.ItemSnapshot(
                i.getProductId().value(),
                i.getProductName().value(),
                i.getVariantId(),
                i.getVariantSku(),
                i.getUnitPrice().amount(),
                i.getQuantity().value()))
            .toList();
        registerEvent(new OrderPlacedEvent(
            id.value(), orderNumber.value(), userId.value(),
            totalAmount.amount(), paymentMethod,
            addressSnapshot, itemSnapshots));
    }

    public void markInventoryReserved(ReservationId reservationId) {
        Objects.requireNonNull(reservationId, "reservationId");
        transition(OrderStatus.INVENTORY_RESERVED);
        this.reservationId = reservationId;
        updatedAt = Instant.now();
        registerEvent(new OrderInventoryReservedEvent(id.value(), orderNumber.value(), reservationId.value()));
    }

    public void markPaymentPending() {
        transition(OrderStatus.PAYMENT_PENDING);
        updatedAt = Instant.now();
    }

    public void markPaymentInitiated(String paymentId) {
        this.paymentId = paymentId;
        updatedAt = Instant.now();
        registerEvent(new OrderPaymentInitiatedEvent(id.value(), orderNumber.value(), paymentId));
    }

    public void markPaymentCompleted() {
        transition(OrderStatus.PAYMENT_COMPLETED);
        updatedAt = Instant.now();
        registerEvent(new OrderPaymentCompletedEvent(id.value(), orderNumber.value(), userId.value()));
    }

    public void confirm() {
        transition(OrderStatus.CONFIRMED);
        this.completedAt = Instant.now();
        updatedAt = Instant.now();
        registerEvent(new OrderConfirmedEvent(id.value(), orderNumber.value(), userId.value(), paymentId));
    }

    public void markShipped(String shipmentId) {
        transition(OrderStatus.SHIPPED);
        this.shipmentId = shipmentId;
        updatedAt = Instant.now();
        registerEvent(new OrderShippedEvent(id.value(), orderNumber.value(), userId.value(), shipmentId));
    }

    public void markDelivered() {
        transition(OrderStatus.DELIVERED);
        this.deliveredAt = Instant.now();
        updatedAt = Instant.now();
        registerEvent(new OrderDeliveredEvent(id.value(), orderNumber.value(), userId.value()));
    }

    public void cancel(String reason) {
        transition(OrderStatus.CANCELLED);
        this.failureReason = reason;
        this.cancelledAt = Instant.now();
        updatedAt = Instant.now();
        registerEvent(new OrderCancelledEvent(id.value(), orderNumber.value(), userId.value(), reason, voucherCode));
    }

    public void markPaymentFailed(String reason) {
        transition(OrderStatus.PAYMENT_FAILED);
        this.failureReason = reason;
        updatedAt = Instant.now();
        registerEvent(new OrderFailedEvent(id.value(), orderNumber.value(), userId.value(), reason));
    }

    public void markFailed(String reason) {
        transition(OrderStatus.FAILED);
        this.failureReason = reason;
        updatedAt = Instant.now();
        registerEvent(new OrderFailedEvent(id.value(), orderNumber.value(), userId.value(), reason));
    }

    public boolean canBeCancelled() { return status.canTransitionTo(OrderStatus.CANCELLED); }
    public boolean canBeReturned()  { return status.canTransitionTo(OrderStatus.RETURN_REQUESTED); }

    public List<OrderItem> getItems() { return Collections.unmodifiableList(items); }

    public void assignId(String id) { this.id = OrderId.of(id); }
    public void addReconstitutedItem(OrderItem item) { items.add(item); }

    private void transition(OrderStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new InvalidOrderStateException(
                    "Cannot transition from " + status + " to " + next);
        }
        this.status = next;
    }
}
