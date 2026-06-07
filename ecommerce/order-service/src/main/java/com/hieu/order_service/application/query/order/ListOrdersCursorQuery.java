package com.hieu.order_service.application.query.order;

import com.hieu.order_service.application.common.Query;
import com.hieu.order_service.application.dto.CursorPageDTO;
import com.hieu.order_service.application.dto.OrderDTO;

public record ListOrdersCursorQuery(String cursor, int limit, String status) implements Query<CursorPageDTO<OrderDTO>> {}
