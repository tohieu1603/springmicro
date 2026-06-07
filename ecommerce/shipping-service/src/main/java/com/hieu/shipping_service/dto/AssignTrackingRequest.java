package com.hieu.shipping_service.dto;

import jakarta.validation.constraints.NotBlank;

/** ADMIN: assign carrier + tracking number to a shipment. */
public record AssignTrackingRequest(@NotBlank String carrier, @NotBlank String trackingNumber) {}
