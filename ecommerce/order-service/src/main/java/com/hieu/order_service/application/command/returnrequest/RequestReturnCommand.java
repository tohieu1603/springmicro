package com.hieu.order_service.application.command.returnrequest;

import com.hieu.order_service.application.common.Command;
import com.hieu.order_service.application.dto.ReturnRequestDTO;

public record RequestReturnCommand(
        String orderId,
        String userId,
        String reason,
        String returnType,
        String images
) implements Command<ReturnRequestDTO> {}
