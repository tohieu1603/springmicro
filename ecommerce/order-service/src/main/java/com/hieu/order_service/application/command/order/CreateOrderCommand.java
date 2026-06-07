package com.hieu.order_service.application.command.order;

import com.hieu.order_service.application.common.Command;
import com.hieu.order_service.application.dto.OrderDTO;

import java.math.BigDecimal;
import java.util.List;

public record CreateOrderCommand(
        String userId,
        List<ItemCmd> items,
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
) implements Command<OrderDTO> {

    public record ItemCmd(
            String productId,
            String productName,
            String variantId,
            String variantSku,
            String variantImage,
            BigDecimal unitPrice,
            int quantity
    ) {}
}
