package com.hieu.order_service.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ReturnRequestDTO(
        String id,
        String orderId,
        String userId,
        String reason,
        String returnType,
        String status,
        BigDecimal refundAmount,
        String adminNote,
        String images,
        Instant createdAt,
        Instant updatedAt
) {}
