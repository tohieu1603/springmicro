package com.hieu.catalog_service.application.handler.variant;

import com.hieu.catalog_service.application.common.QueryHandler;
import com.hieu.catalog_service.application.dto.VariantDTO;
import com.hieu.catalog_service.application.mapper.CatalogDtoMapper;
import com.hieu.catalog_service.application.query.variant.CheckStockQuery;
import com.hieu.catalog_service.domain.exception.VariantNotFoundException;
import com.hieu.catalog_service.domain.model.product.valueobject.Sku;
import com.hieu.catalog_service.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Returns the full {@link VariantDTO} for the SKU in a single fetch.
 * The caller computes {@code available = quantity >= requested} to avoid a second query.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckStockHandler implements QueryHandler<CheckStockQuery, VariantDTO> {

    private final ProductRepository productRepository;
    private final CatalogDtoMapper mapper;

    @Override
    public VariantDTO handle(CheckStockQuery query) {
        var sku = Sku.of(query.sku());
        return productRepository.findBySku(sku)
            .flatMap(p -> p.getVariants().stream().filter(v -> v.getSku().equals(sku)).findFirst())
            .map(mapper::toDto)
            .orElseThrow(() -> VariantNotFoundException.bySku(query.sku()));
    }
}
