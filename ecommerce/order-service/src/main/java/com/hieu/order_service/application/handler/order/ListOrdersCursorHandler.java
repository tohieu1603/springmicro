package com.hieu.order_service.application.handler.order;

import com.hieu.order_service.application.common.CursorCodec;
import com.hieu.order_service.application.common.QueryHandler;
import com.hieu.order_service.application.dto.CursorPageDTO;
import com.hieu.order_service.application.dto.OrderDTO;
import com.hieu.order_service.application.mapper.OrderDtoMapper;
import com.hieu.order_service.application.query.order.ListOrdersCursorQuery;
import com.hieu.order_service.domain.model.order.valueobject.OrderId;
import com.hieu.order_service.domain.model.order.valueobject.OrderStatus;
import com.hieu.order_service.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListOrdersCursorHandler implements QueryHandler<ListOrdersCursorQuery, CursorPageDTO<OrderDTO>> {

    private final OrderRepository orderRepository;
    private final OrderDtoMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public CursorPageDTO<OrderDTO> handle(ListOrdersCursorQuery query) {
        int limit = Math.min(query.limit(), 100);
        var cursor = CursorCodec.decode(query.cursor());
        var status = query.status() != null ? OrderStatus.valueOf(query.status()) : null;

        List<OrderId> ids;
        if (status == null) {
            ids = cursor == null
                    ? orderRepository.findFirstPageIds(limit + 1)
                    : orderRepository.findIdsAfterCursor(cursor.createdAt(), cursor.id(), limit + 1);
        } else {
            ids = cursor == null
                    ? orderRepository.findFirstPageIdsByStatus(status, limit + 1)
                    : orderRepository.findIdsAfterCursorByStatus(status, cursor.createdAt(), cursor.id(), limit + 1);
        }

        var hasMore = ids.size() > limit;
        var page = hasMore ? ids.subList(0, limit) : ids;
        var orders = orderRepository.findAllByIdsWithItems(page).stream().map(mapper::toDto).toList();

        String nextCursor = null;
        if (hasMore && !orders.isEmpty()) {
            var last = orders.getLast();
            nextCursor = CursorCodec.encode(last.createdAt(), last.id());
        }
        return new CursorPageDTO<>(orders, nextCursor, hasMore, orders.size());
    }
}
