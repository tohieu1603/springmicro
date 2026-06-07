package com.hieu.order_service.application.command.returnrequest;

import com.hieu.order_service.application.common.Command;
import com.hieu.order_service.application.dto.ReturnRequestDTO;

import java.math.BigDecimal;

public record CompleteReturnCommand(String returnRequestId, BigDecimal refundAmount)
        implements Command<ReturnRequestDTO> {}
