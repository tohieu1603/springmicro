package com.hieu.catalog_service.application.command.category;

import com.hieu.catalog_service.application.common.Command;
import com.hieu.catalog_service.application.dto.CategoryDTO;

public record CreateCategoryCommand(
        String name,
        String description,
        String parentId,
        int sortOrder,
        String createdBy
) implements Command<CategoryDTO> {}
