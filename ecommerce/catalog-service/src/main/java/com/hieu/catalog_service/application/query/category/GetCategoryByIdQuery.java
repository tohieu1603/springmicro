package com.hieu.catalog_service.application.query.category;

import com.hieu.catalog_service.application.common.Query;
import com.hieu.catalog_service.application.dto.CategoryDTO;

public record GetCategoryByIdQuery(String categoryId) implements Query<CategoryDTO> {}
