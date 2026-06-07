package com.hieu.order_service.application.command.order;

import com.hieu.order_service.application.common.Command;
import com.hieu.order_service.application.dto.OrderDTO;

public record CreateOrderFromCartCommand(
        String userId,
        String recipientName,
        String recipientPhone,
        String street,
        String ward,
        String district,
        String city,
        String country,
        String postalCode,
        String paymentMethod,
        String notes,
        String voucherCode,
        String idempotencyKey,
        String authToken
) implements Command<OrderDTO> {}
