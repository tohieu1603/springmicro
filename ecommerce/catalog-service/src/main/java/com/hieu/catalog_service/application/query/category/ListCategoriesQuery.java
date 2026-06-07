package com.hieu.catalog_service.application.query.category;

import com.hieu.catalog_service.application.common.Query;
import com.hieu.catalog_service.application.dto.CategoryDTO;

import java.util.List;

public record ListCategoriesQuery() implements Query<List<CategoryDTO>> {}
