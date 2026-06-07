package com.hieu.catalog_service.application.handler.attr;

import com.hieu.catalog_service.application.command.attr.CreateAttrCommand;
import com.hieu.catalog_service.application.common.CommandHandler;
import com.hieu.catalog_service.application.common.DomainEventPublisher;
import com.hieu.catalog_service.application.dto.AttrDTO;
import com.hieu.catalog_service.application.mapper.CatalogDtoMapper;
import com.hieu.catalog_service.domain.exception.AttrAlreadyExistsException;
import com.hieu.catalog_service.domain.model.attribute.Attr;
import com.hieu.catalog_service.domain.model.attribute.AttrVal;
import com.hieu.catalog_service.domain.model.attribute.valueobject.AttrType;
import com.hieu.catalog_service.domain.repository.AttrRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateAttrHandler implements CommandHandler<CreateAttrCommand, AttrDTO> {

    private final AttrRepository attrRepository;
    private final CatalogDtoMapper mapper;
    private final DomainEventPublisher eventPublisher;

    @Override
    public AttrDTO handle(CreateAttrCommand cmd) {
        String code = cmd.code().trim().toUpperCase();
        if (attrRepository.existsByCode(code)) throw new AttrAlreadyExistsException(code);

        AttrType type = AttrType.fromString(cmd.type());
        var attr = Attr.create(code, cmd.name(), type);
        if (type == AttrType.SELECT) {
            Optional.ofNullable(cmd.values()).orElse(java.util.List.of())
                .forEach(v -> attr.addValue(AttrVal.create(null, v.val(), v.code())));
        }
        var saved = attrRepository.save(attr);
        saved.raiseCreatedEvent();
        eventPublisher.publishEventsOf(saved);
        return mapper.toDto(saved);
    }
}
