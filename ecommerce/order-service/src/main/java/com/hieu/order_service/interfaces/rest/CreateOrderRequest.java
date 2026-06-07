package com.hieu.order_service.interfaces.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


import java.math.BigDecimal;
import java.util.List;

public record CreateOrderRequest(
        @NotEmpty(message = "items must not be empty")
        @Valid
        List<ItemRequest> items,

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
) {
    public record ItemRequest(
            @NotBlank @Size(max = 36) String productId,
            @Size(max = 200) String productName,
            @Size(max = 36) String variantId,
            @Size(max = 100) String variantSku,
            @Size(max = 500) String variantImage,
            @NotNull @DecimalMin("0") BigDecimal unitPrice,
            @Min(1)          int quantity
    ) {}
}
