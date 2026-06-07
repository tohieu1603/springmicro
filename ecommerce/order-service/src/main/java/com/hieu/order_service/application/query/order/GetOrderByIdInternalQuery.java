package com.hieu.order_service.application.query.order;

import com.hieu.order_service.application.common.Query;
import com.hieu.order_service.application.dto.OrderDTO;

public record GetOrderByIdInternalQuery(String orderId) implements Query<OrderDTO> {}
