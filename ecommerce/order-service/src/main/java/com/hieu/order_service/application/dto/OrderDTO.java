package com.hieu.order_service.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderDTO(
        String id,
        String orderNumber,
        String userId,
        String status,
        List<OrderItemDTO> items,
        BigDecimal subtotalAmount,
        BigDecimal discountAmount,
        BigDecimal shippingFee,
        BigDecimal totalAmount,
        String voucherCode,
        String recipientName,
        String recipientPhone,
        String street,
        String ward,
        String district,
        String city,
        String country,
        String postalCode,
        String notes,
        String paymentMethod,
        String paymentId,
        String reservationId,
        String shipmentId,
        String failureReason,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt,
        Instant deliveredAt,
        Instant cancelledAt,
        // Payment redirect fields (populated after saga)
        String payUrl,
        String qrCodeUrl
) {}
