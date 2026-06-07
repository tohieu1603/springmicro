package com.hieu.catalog_service.application.handler.product;

import com.hieu.catalog_service.application.command.product.UpdateProductCommand;
import com.hieu.catalog_service.application.common.CommandHandler;
import com.hieu.catalog_service.application.common.DomainEventPublisher;
import com.hieu.catalog_service.application.dto.ProductDTO;
import com.hieu.catalog_service.application.mapper.CatalogDtoMapper;
import com.hieu.catalog_service.domain.exception.CategoryNotFoundException;
import com.hieu.catalog_service.domain.exception.ProductNotFoundException;
import com.hieu.catalog_service.domain.model.category.valueobject.CategoryId;
import com.hieu.catalog_service.domain.model.product.Product;
import com.hieu.catalog_service.domain.model.product.valueobject.ProductId;
import com.hieu.catalog_service.domain.repository.CategoryRepository;
import com.hieu.catalog_service.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateProductHandler implements CommandHandler<UpdateProductCommand, ProductDTO> {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CatalogDtoMapper mapper;
    private final DomainEventPublisher eventPublisher;

    @Override
    public ProductDTO handle(UpdateProductCommand cmd) {
        Product product = productRepository.findByIdWithVariants(ProductId.of(cmd.productId()))
                .orElseThrow(() -> new ProductNotFoundException(cmd.productId()));
        if (cmd.categoryId() != null && !categoryRepository.existsById(CategoryId.of(cmd.categoryId()))) {
            throw new CategoryNotFoundException(cmd.categoryId());
        }
        product.update(cmd.name(), cmd.description(),
                cmd.categoryId() != null ? CategoryId.of(cmd.categoryId()) : null,
                cmd.brand(), cmd.updatedBy());
        Product saved = productRepository.save(product);
        eventPublisher.publishEventsOf(saved);
        return mapper.toDto(saved);
    }
}
