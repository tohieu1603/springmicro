package com.hieu.catalog_service.application.handler.product;

import com.hieu.catalog_service.application.command.product.UpdateProductSeoCommand;
import com.hieu.catalog_service.application.common.CommandHandler;
import com.hieu.catalog_service.application.common.DomainEventPublisher;
import com.hieu.catalog_service.application.dto.ProductDTO;
import com.hieu.catalog_service.application.mapper.CatalogDtoMapper;
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
public class UpdateProductSeoHandler implements CommandHandler<UpdateProductSeoCommand, ProductDTO> {

    private final ProductRepository productRepository;
    private final CatalogDtoMapper mapper;
    private final DomainEventPublisher eventPublisher;

    @Override
    public ProductDTO handle(UpdateProductSeoCommand cmd) {
        Product product = productRepository.findByIdWithVariants(ProductId.of(cmd.productId()))
                .orElseThrow(() -> new ProductNotFoundException(cmd.productId()));
        product.updateSeo(cmd.metaTitle(), cmd.metaDescription(), cmd.metaKeywords(), cmd.updatedBy());
        Product saved = productRepository.save(product);
        eventPublisher.publishEventsOf(saved);
        return mapper.toDto(saved);
    }
}
