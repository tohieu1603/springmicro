package com.hieu.order_service.application.query.order;

import com.hieu.order_service.application.common.Query;
import com.hieu.order_service.application.dto.OrderDTO;

public record GetOrderByNumberQuery(String orderNumber, String requestingUserId, boolean isAdmin) implements Query<OrderDTO> {}
