package com.hieu.order_service.application.query.returnrequest;

import com.hieu.order_service.application.common.Query;
import com.hieu.order_service.application.dto.PageDTO;
import com.hieu.order_service.application.dto.ReturnRequestDTO;

public record ListUserReturnRequestsQuery(String userId, int page, int size)
        implements Query<PageDTO<ReturnRequestDTO>> {}
