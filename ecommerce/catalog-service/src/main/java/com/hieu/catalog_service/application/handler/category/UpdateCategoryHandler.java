package com.hieu.catalog_service.application.handler.category;

import com.hieu.catalog_service.application.command.category.UpdateCategoryCommand;
import com.hieu.catalog_service.application.common.CommandHandler;
import com.hieu.catalog_service.application.common.DomainEventPublisher;
import com.hieu.catalog_service.application.dto.CategoryDTO;
import com.hieu.catalog_service.application.mapper.CatalogDtoMapper;
import com.hieu.catalog_service.domain.exception.CategoryNotFoundException;
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
public class UpdateCategoryHandler implements CommandHandler<UpdateCategoryCommand, CategoryDTO> {

    private final CategoryRepository categoryRepository;
    private final CatalogDtoMapper mapper;
    private final DomainEventPublisher eventPublisher;

    @Override
    public CategoryDTO handle(UpdateCategoryCommand cmd) {
        var category = categoryRepository.findById(CategoryId.of(cmd.categoryId()))
            .orElseThrow(() -> new CategoryNotFoundException(cmd.categoryId()));
        CategoryId parent = Optional.ofNullable(cmd.parentId()).map(CategoryId::of).orElse(null);
        if (parent != null) {
            if (!categoryRepository.existsById(parent)) {
                throw new CategoryNotFoundException(parent.value());
            }
            // Indirect cycle guard: re-parenting a category to one of its own descendants
            // would form a cycle (A→B→C→A). The new parent's ancestor chain must not contain
            // the category itself.
            boolean wouldCycle = categoryRepository.findAncestors(parent).stream()
                .anyMatch(c -> c.getId() != null && c.getId().equals(category.getId()));
            if (wouldCycle) {
                throw new IllegalArgumentException(
                    "Cannot move category " + cmd.categoryId() + " under " + parent.value() + ": would create a cycle");
            }
        }
        category.update(CategoryName.of(cmd.name()), CategoryDescription.of(cmd.description()),
            parent, cmd.sortOrder(), cmd.updatedBy());
        var saved = categoryRepository.save(category);
        eventPublisher.publishEventsOf(saved);
        return mapper.toDto(saved);
    }
}
