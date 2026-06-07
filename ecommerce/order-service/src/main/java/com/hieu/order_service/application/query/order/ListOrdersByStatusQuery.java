package com.hieu.order_service.application.query.order;

import com.hieu.order_service.application.common.Query;
import com.hieu.order_service.application.dto.PageDTO;
import com.hieu.order_service.application.dto.OrderDTO;

public record ListOrdersByStatusQuery(String userId, String status, int page, int size) implements Query<PageDTO<OrderDTO>> {}
