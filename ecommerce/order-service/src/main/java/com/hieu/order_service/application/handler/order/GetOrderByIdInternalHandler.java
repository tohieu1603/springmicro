package com.hieu.order_service.application.handler.order;

import com.hieu.order_service.application.common.QueryHandler;
import com.hieu.order_service.application.dto.OrderDTO;
import com.hieu.order_service.application.mapper.OrderDtoMapper;
import com.hieu.order_service.application.query.order.GetOrderByIdInternalQuery;
import com.hieu.order_service.domain.exception.OrderNotFoundException;
import com.hieu.order_service.domain.model.order.valueobject.OrderId;
import com.hieu.order_service.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetOrderByIdInternalHandler implements QueryHandler<GetOrderByIdInternalQuery, OrderDTO> {

    private final OrderRepository orderRepository;
    private final OrderDtoMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public OrderDTO handle(GetOrderByIdInternalQuery query) {
        var order = orderRepository.findById(OrderId.of(query.orderId()))
                .orElseThrow(() -> new OrderNotFoundException(query.orderId()));
        return mapper.toDto(order);
    }
}
