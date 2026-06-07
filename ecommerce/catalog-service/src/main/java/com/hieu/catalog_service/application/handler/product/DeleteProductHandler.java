package com.hieu.catalog_service.application.handler.product;

import com.hieu.catalog_service.application.command.product.DeleteProductCommand;
import com.hieu.catalog_service.application.common.CommandHandler;
import com.hieu.catalog_service.application.common.DomainEventPublisher;
import com.hieu.catalog_service.domain.exception.ProductNotFoundException;
import com.hieu.catalog_service.domain.model.product.valueobject.ProductId;
import com.hieu.catalog_service.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DeleteProductHandler implements CommandHandler<DeleteProductCommand, Void> {

    private final ProductRepository productRepository;
    private final DomainEventPublisher eventPublisher;

    @Override
    public Void handle(DeleteProductCommand cmd) {
        var product = productRepository.findByIdWithVariants(ProductId.of(cmd.productId()))
            .orElseThrow(() -> new ProductNotFoundException(cmd.productId()));
        product.softDelete(cmd.deletedBy());
        eventPublisher.publishEventsOf(productRepository.save(product));
        return null;
    }
}
