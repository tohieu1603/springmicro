package com.hieu.order_service.application.query.returnrequest;

import com.hieu.order_service.application.common.Query;
import com.hieu.order_service.application.dto.ReturnRequestDTO;

public record GetReturnRequestByIdQuery(String returnRequestId, String requestingUserId, boolean isAdmin)
        implements Query<ReturnRequestDTO> {}
