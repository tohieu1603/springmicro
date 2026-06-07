package com.hieu.order_service.infrastructure.persistence.mapper;

import com.hieu.order_service.domain.model.order.ReturnRequest;
import com.hieu.order_service.domain.model.order.valueobject.*;
import com.hieu.order_service.infrastructure.persistence.jpa.entities.ReturnRequestJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ReturnRequestJpaMapper {

    public ReturnRequestJpaEntity toJpa(ReturnRequest rr) {
        var e = new ReturnRequestJpaEntity();
        if (rr.getId() != null) e.setId(rr.getId().value());
        e.setOrderId(rr.getOrderId().value());
        e.setUserId(rr.getUserId().value());
        e.setReason(rr.getReason().value());
        e.setReturnType(rr.getReturnType().name());
        e.setStatus(rr.getStatus().name());
        e.setRefundAmount(rr.getRefundAmount() == null ? null : rr.getRefundAmount().amount());
        e.setAdminNote(rr.getAdminNote());
        e.setImages(rr.getImages());
        e.setCreatedAt(rr.getCreatedAt());
        e.setUpdatedAt(rr.getUpdatedAt());
        return e;
    }

    public ReturnRequest toDomain(ReturnRequestJpaEntity e) {
        return ReturnRequest.reconstitute(
                ReturnRequestId.of(e.getId()),
                OrderId.of(e.getOrderId()),
                UserId.of(e.getUserId()),
                ReturnReason.of(e.getReason()),
                ReturnType.valueOf(e.getReturnType()),
                ReturnStatus.valueOf(e.getStatus()),
                e.getRefundAmount() == null ? null : RefundAmount.of(e.getRefundAmount()),
                e.getAdminNote(),
                e.getImages(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    public void syncGeneratedIds(ReturnRequest rr, ReturnRequestJpaEntity saved) {
        rr.assignId(saved.getId());
    }
}
