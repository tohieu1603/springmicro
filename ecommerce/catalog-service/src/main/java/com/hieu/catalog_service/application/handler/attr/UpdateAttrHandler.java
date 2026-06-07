package com.hieu.catalog_service.application.handler.attr;

import com.hieu.catalog_service.application.command.attr.UpdateAttrCommand;
import com.hieu.catalog_service.application.common.CommandHandler;
import com.hieu.catalog_service.application.common.DomainEventPublisher;
import com.hieu.catalog_service.application.dto.AttrDTO;
import com.hieu.catalog_service.application.mapper.CatalogDtoMapper;
import com.hieu.catalog_service.domain.exception.AttrNotFoundException;
import com.hieu.catalog_service.domain.model.attribute.valueobject.AttrId;
import com.hieu.catalog_service.domain.model.attribute.valueobject.AttrType;
import com.hieu.catalog_service.domain.repository.AttrRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateAttrHandler implements CommandHandler<UpdateAttrCommand, AttrDTO> {

    private final AttrRepository attrRepository;
    private final CatalogDtoMapper mapper;
    private final DomainEventPublisher eventPublisher;

    @Override
    public AttrDTO handle(UpdateAttrCommand cmd) {
        var attr = attrRepository.findById(AttrId.of(cmd.attrId()))
            .orElseThrow(() -> new AttrNotFoundException(cmd.attrId()));
        attr.update(cmd.name(), cmd.type() != null ? AttrType.fromString(cmd.type()) : null, cmd.sortOrder());
        var saved = attrRepository.save(attr);
        eventPublisher.publishEventsOf(saved);
        return mapper.toDto(saved);
    }
}
