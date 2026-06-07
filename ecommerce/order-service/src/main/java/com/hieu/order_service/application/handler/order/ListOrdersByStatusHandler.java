package com.hieu.order_service.application.handler.order;

import com.hieu.order_service.application.common.QueryHandler;
import com.hieu.order_service.application.dto.OrderDTO;
import com.hieu.order_service.application.dto.PageDTO;
import com.hieu.order_service.application.mapper.OrderDtoMapper;
import com.hieu.order_service.application.query.order.ListOrdersByStatusQuery;
import com.hieu.order_service.domain.model.order.valueobject.OrderStatus;
import com.hieu.order_service.domain.model.order.valueobject.UserId;
import com.hieu.order_service.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListOrdersByStatusHandler implements QueryHandler<ListOrdersByStatusQuery, PageDTO<OrderDTO>> {

    private final OrderRepository orderRepository;
    private final OrderDtoMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public PageDTO<OrderDTO> handle(ListOrdersByStatusQuery query) {
        var pageable = PageRequest.of(query.page(), query.size(), Sort.by("createdAt").descending());
        var status = OrderStatus.valueOf(query.status());
        var userId = UserId.of(query.userId());
        var page = orderRepository.findByUserIdAndStatus(userId, status, pageable);
        var content = page.getContent().stream().map(mapper::toDto).toList();
        return new PageDTO<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
