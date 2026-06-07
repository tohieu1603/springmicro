package com.hieu.order_service.application.command.returnrequest;

import com.hieu.order_service.application.common.Command;
import com.hieu.order_service.application.dto.ReturnRequestDTO;

public record RejectReturnCommand(String returnRequestId, String adminNote)
        implements Command<ReturnRequestDTO> {}
