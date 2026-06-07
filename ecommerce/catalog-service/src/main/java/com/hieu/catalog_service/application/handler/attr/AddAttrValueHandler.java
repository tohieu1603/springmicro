package com.hieu.catalog_service.application.handler.attr;

import com.hieu.catalog_service.application.command.attr.AddAttrValueCommand;
import com.hieu.catalog_service.application.common.CommandHandler;
import com.hieu.catalog_service.application.common.DomainEventPublisher;
import com.hieu.catalog_service.application.dto.AttrDTO;
import com.hieu.catalog_service.application.mapper.CatalogDtoMapper;
import com.hieu.catalog_service.domain.exception.AttrNotFoundException;
import com.hieu.catalog_service.domain.model.attribute.AttrVal;
import com.hieu.catalog_service.domain.model.attribute.valueobject.AttrId;
import com.hieu.catalog_service.domain.repository.AttrRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AddAttrValueHandler implements CommandHandler<AddAttrValueCommand, AttrDTO> {

    private final AttrRepository attrRepository;
    private final CatalogDtoMapper mapper;
    private final DomainEventPublisher eventPublisher;

    @Override
    public AttrDTO handle(AddAttrValueCommand cmd) {
        var attr = attrRepository.findById(AttrId.of(cmd.attrId()))
            .orElseThrow(() -> new AttrNotFoundException(cmd.attrId()));
        attr.addValue(AttrVal.create(attr.getId().value(), cmd.val(), cmd.code()));
        var saved = attrRepository.save(attr);
        eventPublisher.publishEventsOf(saved);
        return mapper.toDto(saved);
    }
}
