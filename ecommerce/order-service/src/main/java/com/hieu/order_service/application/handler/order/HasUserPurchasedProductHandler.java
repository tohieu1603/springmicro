package com.hieu.order_service.application.handler.order;

import com.hieu.order_service.application.common.QueryHandler;
import com.hieu.order_service.application.query.order.HasUserPurchasedProductQuery;
import com.hieu.order_service.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HasUserPurchasedProductHandler implements QueryHandler<HasUserPurchasedProductQuery, Boolean> {

    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public Boolean handle(HasUserPurchasedProductQuery query) {
        return orderRepository.existsByUserIdAndProductId(query.userId(), query.productId());
    }
}
