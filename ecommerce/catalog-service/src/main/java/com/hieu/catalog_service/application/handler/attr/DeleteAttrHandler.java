package com.hieu.catalog_service.application.handler.attr;

import com.hieu.catalog_service.application.command.attr.DeleteAttrCommand;
import com.hieu.catalog_service.application.common.CommandHandler;
import com.hieu.catalog_service.application.common.DomainEventPublisher;
import com.hieu.catalog_service.domain.exception.AttrNotFoundException;
import com.hieu.catalog_service.domain.model.attribute.valueobject.AttrId;
import com.hieu.catalog_service.domain.repository.AttrRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DeleteAttrHandler implements CommandHandler<DeleteAttrCommand, Void> {

    private final AttrRepository attrRepository;
    private final DomainEventPublisher eventPublisher;

    @Override
    public Void handle(DeleteAttrCommand cmd) {
        var attr = attrRepository.findById(AttrId.of(cmd.attrId()))
            .orElseThrow(() -> new AttrNotFoundException(cmd.attrId()));
        attr.markDeleted();
        attrRepository.delete(attr);
        eventPublisher.publishEventsOf(attr);
        return null;
    }
}
