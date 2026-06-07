package com.hieu.catalog_service.application.handler.category;

import com.hieu.catalog_service.application.common.QueryHandler;
import com.hieu.catalog_service.application.dto.CategoryDTO;
import com.hieu.catalog_service.application.mapper.CatalogDtoMapper;
import com.hieu.catalog_service.application.query.category.GetCategoryByIdQuery;
import com.hieu.catalog_service.domain.exception.CategoryNotFoundException;
import com.hieu.catalog_service.domain.model.category.valueobject.CategoryId;
import com.hieu.catalog_service.domain.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetCategoryByIdHandler implements QueryHandler<GetCategoryByIdQuery, CategoryDTO> {

    private final CategoryRepository categoryRepository;
    private final CatalogDtoMapper mapper;

    @Override
    public CategoryDTO handle(GetCategoryByIdQuery query) {
        return categoryRepository.findById(CategoryId.of(query.categoryId()))
            .map(mapper::toDto)
            .orElseThrow(() -> new CategoryNotFoundException(query.categoryId()));
    }
}
