package com.hieu.order_service.application.handler.returnrequest;

import com.hieu.order_service.application.common.QueryHandler;
import com.hieu.order_service.application.dto.ReturnRequestDTO;
import com.hieu.order_service.application.mapper.OrderDtoMapper;
import com.hieu.order_service.application.query.returnrequest.GetReturnRequestByIdQuery;
import com.hieu.order_service.domain.exception.ReturnRequestNotFoundException;
import com.hieu.order_service.domain.model.order.valueobject.ReturnRequestId;
import com.hieu.order_service.domain.repository.ReturnRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetReturnRequestByIdHandler implements QueryHandler<GetReturnRequestByIdQuery, ReturnRequestDTO> {

    private final ReturnRequestRepository returnRequestRepository;
    private final OrderDtoMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public ReturnRequestDTO handle(GetReturnRequestByIdQuery query) {
        var rr = returnRequestRepository.findById(ReturnRequestId.of(query.returnRequestId()))
                .orElseThrow(() -> new ReturnRequestNotFoundException(query.returnRequestId()));
        if (!query.isAdmin() && !rr.getUserId().value().equals(query.requestingUserId())) {
            throw new AccessDeniedException("Access denied");
        }
        return mapper.toReturnDto(rr);
    }
}
