package com.hieu.catalog_service.application.handler.category;

import com.hieu.catalog_service.application.command.category.DeleteCategoryCommand;
import com.hieu.catalog_service.application.common.CommandHandler;
import com.hieu.catalog_service.application.common.DomainEventPublisher;
import com.hieu.catalog_service.domain.exception.CategoryNotFoundException;
import com.hieu.catalog_service.domain.model.category.valueobject.CategoryId;
import com.hieu.catalog_service.domain.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DeleteCategoryHandler implements CommandHandler<DeleteCategoryCommand, Void> {

    private final CategoryRepository categoryRepository;
    private final DomainEventPublisher eventPublisher;

    @Override
    public Void handle(DeleteCategoryCommand cmd) {
        var category = categoryRepository.findById(CategoryId.of(cmd.categoryId()))
            .orElseThrow(() -> new CategoryNotFoundException(cmd.categoryId()));
        category.softDelete(cmd.deletedBy());
        eventPublisher.publishEventsOf(categoryRepository.save(category));
        return null;
    }
}
