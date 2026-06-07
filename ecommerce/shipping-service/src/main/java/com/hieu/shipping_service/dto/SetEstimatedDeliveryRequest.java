package com.hieu.shipping_service.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/** ADMIN: update estimated delivery date. */
public record SetEstimatedDeliveryRequest(@NotNull Instant estimatedDeliveryDate) {}
