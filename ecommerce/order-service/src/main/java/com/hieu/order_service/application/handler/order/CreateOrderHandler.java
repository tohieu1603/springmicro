package com.hieu.order_service.application.handler.order;

import com.hieu.order_service.application.command.order.CreateOrderCommand;
import com.hieu.order_service.application.common.CommandHandler;
import com.hieu.order_service.application.common.DomainEventPublisher;
import com.hieu.order_service.application.common.ValidationException;
import com.hieu.order_service.application.dto.OrderDTO;
import com.hieu.order_service.application.mapper.OrderDtoMapper;
import com.hieu.order_service.application.saga.OrderSagaOrchestrator;
import com.hieu.order_service.application.service.IdempotencyService;
import com.hieu.order_service.application.service.OrderNumberService;
import com.hieu.order_service.domain.model.order.Order;
import com.hieu.order_service.domain.model.order.OrderItem;
import com.hieu.order_service.domain.model.order.valueobject.*;
import com.hieu.order_service.domain.repository.OrderRepository;
import com.hieu.order_service.domain.service.OrderDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Handles direct-item order creation. Persists order, then delegates to saga for
 * inventory reservation and payment initiation.
 */
@Service
@RequiredArgsConstructor
public class CreateOrderHandler implements CommandHandler<CreateOrderCommand, OrderDTO> {

    private static final String VALIDATION_REQUIRED = "required";


    private final OrderRepository orderRepository;
    private final OrderDomainService domainService;
    private final OrderDtoMapper mapper;
    private final DomainEventPublisher eventPublisher;
    private final OrderSagaOrchestrator saga;
    private final IdempotencyService idempotencyService;
    private final OrderNumberService orderNumberService;

    @Override
    public OrderDTO handle(CreateOrderCommand cmd) {
        validate(cmd);

        // Idempotency check
        if (cmd.idempotencyKey() != null) {
            var existing = idempotencyService.checkOrCreate(cmd.userId(), cmd.idempotencyKey());
            if (existing.isPresent()) return existing.get();
        }

        var order = buildAndSave(cmd);
        var dto = saga.executeCreateOrderSaga(order.getId().value(), cmd.authToken());
        // Persist final DTO so retries within TTL return the cached response instead
        // of being rejected as "PROCESSING" forever.
        if (cmd.idempotencyKey() != null) {
            idempotencyService.markCompleted(cmd.idempotencyKey(), order.getId().value(), dto);
        }
        return dto;
    }

    @Transactional
    public Order buildAndSave(CreateOrderCommand cmd) {
        var items = cmd.items().stream().map(ic -> OrderItem.create(
                ProductId.of(ic.productId()),
                ProductName.of(ic.productName()),
                ic.variantId(),
                ic.variantSku(),
                ic.variantImage(),
                Money.of(ic.unitPrice()),
                Quantity.of(ic.quantity())
        )).toList();

        var order = domainService.createOrder(
                UserId.of(cmd.userId()),
                OrderNumber.of(orderNumberService.next()),
                RecipientName.of(cmd.recipientName()),
                RecipientPhone.of(cmd.recipientPhone()),
                ShippingAddress.of(cmd.street(), cmd.ward(), cmd.district(), cmd.city(), cmd.country(), cmd.postalCode()),
                cmd.paymentMethod(), cmd.notes(), cmd.voucherCode(), cmd.idempotencyKey(),
                items, cmd.userId()
        );

        var saved = orderRepository.save(order);
        saved.raiseCreatedEvent();
        eventPublisher.publishEventsOf(saved);
        return saved;
    }

    private void validate(CreateOrderCommand cmd) {
        var errors = new LinkedHashMap<String, String>();
        if (cmd.userId() == null || cmd.userId().isBlank()) errors.put("userId", VALIDATION_REQUIRED);
        if (cmd.items() == null || cmd.items().isEmpty()) errors.put("items", "at least one item required");
        if (cmd.recipientName() == null || cmd.recipientName().isBlank()) errors.put("recipientName", VALIDATION_REQUIRED);
        if (cmd.recipientPhone() == null || cmd.recipientPhone().isBlank()) errors.put("recipientPhone", VALIDATION_REQUIRED);
        if (cmd.paymentMethod() == null || cmd.paymentMethod().isBlank()) errors.put("paymentMethod", VALIDATION_REQUIRED);
        if (!errors.isEmpty()) throw new ValidationException("Invalid order payload", errors);
    }
}
