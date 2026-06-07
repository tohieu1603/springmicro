package com.hieu.catalog_service.application.handler.variant;

import com.hieu.catalog_service.application.command.variant.RemoveVariantCommand;
import com.hieu.catalog_service.application.common.CommandHandler;
import com.hieu.catalog_service.application.common.DomainEventPublisher;
import com.hieu.catalog_service.domain.exception.ProductNotFoundException;
import com.hieu.catalog_service.domain.model.product.valueobject.ProductId;
import com.hieu.catalog_service.domain.model.product.valueobject.VariantId;
import com.hieu.catalog_service.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RemoveVariantHandler implements CommandHandler<RemoveVariantCommand, Void> {

    private final ProductRepository productRepository;
    private final DomainEventPublisher eventPublisher;

    @Override
    public Void handle(RemoveVariantCommand cmd) {
        var product = productRepository.findByIdWithVariants(ProductId.of(cmd.productId()))
            .orElseThrow(() -> new ProductNotFoundException(cmd.productId()));
        product.removeVariant(VariantId.of(cmd.variantId()), cmd.deletedBy());
        eventPublisher.publishEventsOf(productRepository.save(product));
        return null;
    }
}
