package com.hieu.catalog_service.application.handler.product;

import com.hieu.catalog_service.application.common.QueryHandler;
import com.hieu.catalog_service.application.dto.ProductDTO;
import com.hieu.catalog_service.application.mapper.CatalogDtoMapper;
import com.hieu.catalog_service.application.query.product.GetProductByIdQuery;
import com.hieu.catalog_service.domain.exception.ProductNotFoundException;
import com.hieu.catalog_service.domain.model.product.valueobject.ProductId;
import com.hieu.catalog_service.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetProductByIdHandler implements QueryHandler<GetProductByIdQuery, ProductDTO> {

    private final ProductRepository productRepository;
    private final CatalogDtoMapper mapper;

    @Override
    public ProductDTO handle(GetProductByIdQuery query) {
        return productRepository.findByIdWithVariants(ProductId.of(query.productId()))
            .map(mapper::toDto)
            .orElseThrow(() -> new ProductNotFoundException(query.productId()));
    }
}
