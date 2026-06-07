package com.hieu.order_service.domain.model.order.valueobject;

import java.util.Objects;

public record ReservationId(String value) {

    public ReservationId { Objects.requireNonNull(value, "ReservationId value"); }

    public static ReservationId of(String value) { return new ReservationId(value); }
}
