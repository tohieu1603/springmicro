package com.hieu.order_service.application.handler.returnrequest;

import com.hieu.order_service.application.command.returnrequest.CompleteReturnCommand;
import com.hieu.order_service.application.common.CommandHandler;
import com.hieu.order_service.application.common.DomainEventPublisher;
import com.hieu.order_service.application.dto.ReturnRequestDTO;
import com.hieu.order_service.application.mapper.OrderDtoMapper;
import com.hieu.order_service.domain.exception.ReturnRequestNotFoundException;
import com.hieu.order_service.domain.model.order.valueobject.RefundAmount;
import com.hieu.order_service.domain.model.order.valueobject.ReturnRequestId;
import com.hieu.order_service.domain.repository.ReturnRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompleteReturnHandler implements CommandHandler<CompleteReturnCommand, ReturnRequestDTO> {

    private final ReturnRequestRepository returnRequestRepository;
    private final OrderDtoMapper mapper;
    private final DomainEventPublisher eventPublisher;

    @Override
    @Transactional
    public ReturnRequestDTO handle(CompleteReturnCommand cmd) {
        var rr = returnRequestRepository.findById(ReturnRequestId.of(cmd.returnRequestId()))
                .orElseThrow(() -> new ReturnRequestNotFoundException(cmd.returnRequestId()));
        rr.complete(RefundAmount.of(cmd.refundAmount()));
        var saved = returnRequestRepository.save(rr);
        eventPublisher.publishEventsOf(saved);
        return mapper.toReturnDto(saved);
    }
}
