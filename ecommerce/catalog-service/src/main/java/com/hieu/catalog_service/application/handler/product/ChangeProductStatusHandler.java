package com.hieu.catalog_service.application.handler.product;

import com.hieu.catalog_service.application.command.product.ChangeProductStatusCommand;
import com.hieu.catalog_service.application.common.CommandHandler;
import com.hieu.catalog_service.application.common.DomainEventPublisher;
import com.hieu.catalog_service.domain.exception.ProductNotFoundException;
import com.hieu.catalog_service.domain.model.product.Product;
import com.hieu.catalog_service.domain.model.product.valueobject.ProductId;
import com.hieu.catalog_service.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ChangeProductStatusHandler implements CommandHandler<ChangeProductStatusCommand, Void> {

    private final ProductRepository productRepository;
    private final DomainEventPublisher eventPublisher;

    @Override
    public Void handle(ChangeProductStatusCommand cmd) {
        Product product = productRepository.findByIdWithVariants(ProductId.of(cmd.productId()))
                .orElseThrow(() -> new ProductNotFoundException(cmd.productId()));
        switch (cmd.transition()) {
            case ACTIVATE -> product.activate(cmd.updatedBy());
            case DEACTIVATE -> product.deactivate(cmd.updatedBy());
            case DRAFT -> product.moveToDraft(cmd.updatedBy());
        }
        Product saved = productRepository.save(product);
        eventPublisher.publishEventsOf(saved);
        return null;
    }
}
