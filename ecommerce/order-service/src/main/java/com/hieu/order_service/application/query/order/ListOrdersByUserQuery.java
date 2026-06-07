package com.hieu.order_service.application.query.order;

import com.hieu.order_service.application.common.Query;
import com.hieu.order_service.application.dto.CursorPageDTO;
import com.hieu.order_service.application.dto.OrderDTO;

public record ListOrdersByUserQuery(String userId, String cursor, int limit) implements Query<CursorPageDTO<OrderDTO>> {}
