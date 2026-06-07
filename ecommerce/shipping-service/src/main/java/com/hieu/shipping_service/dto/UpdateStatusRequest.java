package com.hieu.shipping_service.dto;

import jakarta.validation.constraints.NotBlank;

/** ADMIN: update shipment status with optional notes. */
public record UpdateStatusRequest(@NotBlank String status, String notes) {}
