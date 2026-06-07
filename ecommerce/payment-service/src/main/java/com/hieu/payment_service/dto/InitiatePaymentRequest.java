package com.hieu.payment_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class InitiatePaymentRequest {

    @NotBlank
    private String orderId;

    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Currency must not be blank")
    private String currency;

    @NotNull(message = "Payment method must not be null")
    private String method;

    /** Optional — when provided makes the endpoint idempotent. */
    private String idempotencyKey;
}
