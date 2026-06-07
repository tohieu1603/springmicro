package com.hieu.order_service.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateOrderFromCartRequest(
        @NotBlank @Size(max = 100) String recipientName,
        @NotBlank @Pattern(regexp = "[0-9+\\-\\s]{8,20}", message = "invalid phone")
        String recipientPhone,

        @NotBlank @Size(max = 200) String street,
        @NotBlank @Size(max = 100) String ward,
        @NotBlank @Size(max = 100) String district,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 50)  String country,
        @Size(max = 20)            String postalCode,

        @NotBlank @Size(max = 20)  String paymentMethod,
        @Size(max = 500)           String notes,
        @Size(max = 50)            String voucherCode
) {}
