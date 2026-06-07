package com.hieu.order_service.domain.repository;

import com.hieu.order_service.domain.model.order.ReturnRequest;
import com.hieu.order_service.domain.model.order.valueobject.OrderId;
import com.hieu.order_service.domain.model.order.valueobject.ReturnRequestId;
import com.hieu.order_service.domain.model.order.valueobject.UserId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ReturnRequestRepository {
    ReturnRequest save(ReturnRequest returnRequest);
    Optional<ReturnRequest> findById(ReturnRequestId id);
    Page<ReturnRequest> findByUserId(UserId userId, Pageable pageable);
    Optional<ReturnRequest> findByOrderId(OrderId orderId);
}
