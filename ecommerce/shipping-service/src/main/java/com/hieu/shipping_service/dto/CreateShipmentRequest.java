package com.hieu.shipping_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Request body for creating a new shipment. */
public record CreateShipmentRequest(

        @NotBlank String orderId,
        @NotNull  String userId,

        String carrier,

        @NotBlank String recipientName,

        @NotBlank
        @Pattern(
            regexp = "^(\\+84|0)[0-9]{9,10}$",
            message = "must be a valid Vietnamese phone number"
        )
        String recipientPhone,

        @NotBlank String addressLine,

        String ward,
        String district,

        @NotBlank String city,

        String country,
        String notes
) {}
