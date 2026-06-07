package com.hieu.order_service.application.handler.order;

import com.hieu.order_service.application.common.QueryHandler;
import com.hieu.order_service.application.dto.OrderDTO;
import com.hieu.order_service.application.mapper.OrderDtoMapper;
import com.hieu.order_service.application.query.order.GetOrderByNumberQuery;
import com.hieu.order_service.domain.exception.OrderNotFoundException;
import com.hieu.order_service.domain.model.order.valueobject.OrderNumber;
import com.hieu.order_service.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetOrderByNumberHandler implements QueryHandler<GetOrderByNumberQuery, OrderDTO> {

    private final OrderRepository orderRepository;
    private final OrderDtoMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public OrderDTO handle(GetOrderByNumberQuery query) {
        var order = orderRepository.findByOrderNumber(OrderNumber.of(query.orderNumber()))
                .orElseThrow(() -> new OrderNotFoundException(query.orderNumber()));
        if (!query.isAdmin() && !order.getUserId().value().equals(query.requestingUserId())) {
            throw new AccessDeniedException("Access denied");
        }
        return mapper.toDto(order);
    }
}
