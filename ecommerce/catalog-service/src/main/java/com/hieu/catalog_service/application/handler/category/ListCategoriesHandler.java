package com.hieu.catalog_service.application.handler.category;

import com.hieu.catalog_service.application.common.QueryHandler;
import com.hieu.catalog_service.application.dto.CategoryDTO;
import com.hieu.catalog_service.application.mapper.CatalogDtoMapper;
import com.hieu.catalog_service.application.query.category.ListCategoriesQuery;
import com.hieu.catalog_service.domain.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListCategoriesHandler implements QueryHandler<ListCategoriesQuery, List<CategoryDTO>> {

    private final CategoryRepository categoryRepository;
    private final CatalogDtoMapper mapper;

    @Override
    public List<CategoryDTO> handle(ListCategoriesQuery query) {
        return categoryRepository.findAllActive().stream().map(mapper::toDto).toList();
    }
}
