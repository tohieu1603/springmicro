package com.hieu.order_service.application.handler.returnrequest;

import com.hieu.order_service.application.common.QueryHandler;
import com.hieu.order_service.application.dto.PageDTO;
import com.hieu.order_service.application.dto.ReturnRequestDTO;
import com.hieu.order_service.application.mapper.OrderDtoMapper;
import com.hieu.order_service.application.query.returnrequest.ListUserReturnRequestsQuery;
import com.hieu.order_service.domain.model.order.valueobject.UserId;
import com.hieu.order_service.domain.repository.ReturnRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListUserReturnRequestsHandler implements QueryHandler<ListUserReturnRequestsQuery, PageDTO<ReturnRequestDTO>> {

    private final ReturnRequestRepository returnRequestRepository;
    private final OrderDtoMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public PageDTO<ReturnRequestDTO> handle(ListUserReturnRequestsQuery query) {
        var pageable = PageRequest.of(query.page(), query.size(), Sort.by("createdAt").descending());
        var page = returnRequestRepository.findByUserId(UserId.of(query.userId()), pageable);
        var content = page.getContent().stream().map(mapper::toReturnDto).toList();
        return new PageDTO<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
