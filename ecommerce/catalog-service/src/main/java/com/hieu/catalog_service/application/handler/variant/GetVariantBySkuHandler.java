package com.hieu.catalog_service.application.handler.variant;

import com.hieu.catalog_service.application.common.QueryHandler;
import com.hieu.catalog_service.application.dto.VariantDTO;
import com.hieu.catalog_service.application.mapper.CatalogDtoMapper;
import com.hieu.catalog_service.application.query.variant.GetVariantBySkuQuery;
import com.hieu.catalog_service.domain.exception.VariantNotFoundException;
import com.hieu.catalog_service.domain.model.product.valueobject.Sku;
import com.hieu.catalog_service.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetVariantBySkuHandler implements QueryHandler<GetVariantBySkuQuery, VariantDTO> {

    private final ProductRepository productRepository;
    private final CatalogDtoMapper mapper;

    @Override
    public VariantDTO handle(GetVariantBySkuQuery query) {
        var sku = Sku.of(query.sku());
        var product = productRepository.findBySku(sku)
            .orElseThrow(() -> VariantNotFoundException.bySku(query.sku()));
        return product.getVariants().stream()
            .filter(v -> v.getSku().equals(sku))
            .findFirst().map(mapper::toDto)
            .orElseThrow(() -> VariantNotFoundException.bySku(query.sku()));
    }
}
