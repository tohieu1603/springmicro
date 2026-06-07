package com.hieu.catalog_service.application.handler.category;

import com.hieu.catalog_service.application.command.category.CreateCategoryCommand;
import com.hieu.catalog_service.application.common.CommandHandler;
import com.hieu.catalog_service.application.common.DomainEventPublisher;
import com.hieu.catalog_service.application.dto.CategoryDTO;
import com.hieu.catalog_service.application.mapper.CatalogDtoMapper;
import com.hieu.catalog_service.domain.exception.CategoryAlreadyExistsException;
import com.hieu.catalog_service.domain.exception.CategoryNotFoundException;
import com.hieu.catalog_service.domain.model.category.Category;
import com.hieu.catalog_service.domain.model.category.valueobject.CategoryDescription;
import com.hieu.catalog_service.domain.model.category.valueobject.CategoryId;
import com.hieu.catalog_service.domain.model.category.valueobject.CategoryName;
import com.hieu.catalog_service.domain.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateCategoryHandler implements CommandHandler<CreateCategoryCommand, CategoryDTO> {

    private final CategoryRepository categoryRepository;
    private final CatalogDtoMapper mapper;
    private final DomainEventPublisher eventPublisher;

    @Override
    public CategoryDTO handle(CreateCategoryCommand cmd) {
        var name = CategoryName.of(cmd.name());
        if (categoryRepository.existsByName(name)) throw new CategoryAlreadyExistsException(name.value());

        CategoryId parent = Optional.ofNullable(cmd.parentId()).map(CategoryId::of).orElse(null);
        if (parent != null && !categoryRepository.existsById(parent)) {
            throw new CategoryNotFoundException(parent.value());
        }
        var category = Category.create(name, CategoryDescription.of(cmd.description()),
            parent, cmd.sortOrder(), cmd.createdBy());
        var saved = categoryRepository.save(category);
        saved.raiseCreatedEvent();
        eventPublisher.publishEventsOf(saved);
        return mapper.toDto(saved);
    }
}
