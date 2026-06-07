package com.hieu.catalog_service.application.handler.variant;

import com.hieu.catalog_service.application.command.variant.UpdateVariantPricingCommand;
import com.hieu.catalog_service.application.common.CommandHandler;
import com.hieu.catalog_service.application.common.DomainEventPublisher;
import com.hieu.catalog_service.application.dto.VariantDTO;
import com.hieu.catalog_service.application.mapper.CatalogDtoMapper;
import com.hieu.catalog_service.domain.exception.ProductNotFoundException;
import com.hieu.catalog_service.domain.model.product.valueobject.Money;
import com.hieu.catalog_service.domain.model.product.valueobject.ProductId;
import com.hieu.catalog_service.domain.model.product.valueobject.VariantId;
import com.hieu.catalog_service.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateVariantPricingHandler implements CommandHandler<UpdateVariantPricingCommand, VariantDTO> {

    private final ProductRepository productRepository;
    private final CatalogDtoMapper mapper;
    private final DomainEventPublisher eventPublisher;

    @Override
    public VariantDTO handle(UpdateVariantPricingCommand cmd) {
        var product = productRepository.findByIdWithVariants(ProductId.of(cmd.productId()))
            .orElseThrow(() -> new ProductNotFoundException(cmd.productId()));
        var variantId = VariantId.of(cmd.variantId());
        product.updateVariantPricing(variantId, Money.of(cmd.price()), Money.ofNullable(cmd.cost()),
            Money.ofNullable(cmd.salePrice()), cmd.updatedBy());
        var saved = productRepository.save(product);
        eventPublisher.publishEventsOf(saved);
        return saved.getVariants().stream()
            .filter(v -> variantId.equals(v.getId()))
            .findFirst().map(mapper::toDto).orElseThrow();
    }
}
