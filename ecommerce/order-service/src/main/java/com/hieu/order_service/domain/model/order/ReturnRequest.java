package com.hieu.order_service.domain.model.order;

import com.hieu.order_service.domain.events.returnrequest.OrderReturnApprovedEvent;
import com.hieu.order_service.domain.events.returnrequest.OrderReturnRejectedEvent;
import com.hieu.order_service.domain.events.returnrequest.OrderReturnRequestedEvent;
import com.hieu.order_service.domain.events.returnrequest.OrderReturnedEvent;
import com.hieu.order_service.domain.model.order.valueobject.*;
import com.hieu.order_service.domain.shared.AggregateRoot;
import lombok.Getter;

import java.time.Instant;

/** Aggregate representing a return/refund request. */
@Getter
public class ReturnRequest extends AggregateRoot {

    private ReturnRequestId id;
    private final OrderId orderId;
    private final UserId userId;
    private final ReturnReason reason;
    private final ReturnType returnType;
    private ReturnStatus status;
    private RefundAmount refundAmount;
    private String adminNote;
    private String images; // JSON TEXT
    private final Instant createdAt;
    private Instant updatedAt;

    private ReturnRequest(ReturnRequestId id, OrderId orderId, UserId userId,
                          ReturnReason reason, ReturnType returnType, ReturnStatus status,
                          RefundAmount refundAmount, String adminNote, String images,
                          Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.orderId = orderId;
        this.userId = userId;
        this.reason = reason;
        this.returnType = returnType;
        this.status = status;
        this.refundAmount = refundAmount;
        this.adminNote = adminNote;
        this.images = images;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ReturnRequest create(OrderId orderId, UserId userId, ReturnReason reason,
                                       ReturnType returnType, String images) {
        var rr = new ReturnRequest(null, orderId, userId, reason, returnType,
                ReturnStatus.PENDING, null, null, images, Instant.now(), Instant.now());
        return rr;
    }

    public static ReturnRequest reconstitute(ReturnRequestId id, OrderId orderId, UserId userId,
                                              ReturnReason reason, ReturnType returnType, ReturnStatus status,
                                              RefundAmount refundAmount, String adminNote, String images,
                                              Instant createdAt, Instant updatedAt) {
        return new ReturnRequest(id, orderId, userId, reason, returnType, status,
                refundAmount, adminNote, images, createdAt, updatedAt);
    }

    public void raiseCreatedEvent() {
        registerEvent(new OrderReturnRequestedEvent(orderId.value(), id == null ? null : id.value(),
                userId.value(), reason.value()));
    }

    public void approve(String adminNote) {
        if (status != ReturnStatus.PENDING) throw new IllegalStateException("Can only approve PENDING return requests");
        this.status = ReturnStatus.APPROVED;
        this.adminNote = adminNote;
        this.updatedAt = Instant.now();
        registerEvent(new OrderReturnApprovedEvent(orderId.value(), id == null ? null : id.value(), userId.value()));
    }

    public void reject(String adminNote) {
        if (status != ReturnStatus.PENDING) throw new IllegalStateException("Can only reject PENDING return requests");
        this.status = ReturnStatus.REJECTED;
        this.adminNote = adminNote;
        this.updatedAt = Instant.now();
        registerEvent(new OrderReturnRejectedEvent(orderId.value(), id == null ? null : id.value(), userId.value()));
    }

    public void complete(RefundAmount refund) {
        if (status != ReturnStatus.APPROVED) throw new IllegalStateException("Can only complete APPROVED return requests");
        this.status = ReturnStatus.COMPLETED;
        this.refundAmount = refund;
        this.updatedAt = Instant.now();
        registerEvent(new OrderReturnedEvent(orderId.value(), id == null ? null : id.value(),
                userId.value(), refund == null ? null : refund.amount()));
    }

    public void assignId(String id) { this.id = ReturnRequestId.of(id); }
}
