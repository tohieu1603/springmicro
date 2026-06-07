package com.hieu.catalog_service.application.command.category;

import com.hieu.catalog_service.application.common.Command;
import com.hieu.catalog_service.application.dto.CategoryDTO;

public record UpdateCategoryCommand(
        String categoryId,
        String name,
        String description,
        String parentId,
        int sortOrder,
        String updatedBy
) implements Command<CategoryDTO> {}
