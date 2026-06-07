package com.hieu.order_service.application.command.order;

import com.hieu.order_service.application.common.Command;
import com.hieu.order_service.application.dto.OrderDTO;

public record CancelOrderCommand(String orderId, String reason, String requestingUserId, boolean isAdmin)
        implements Command<OrderDTO> {}
