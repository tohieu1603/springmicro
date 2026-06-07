package com.hieu.catalog_service.application.handler.attr;

import com.hieu.catalog_service.application.command.attr.RemoveAttrValueCommand;
import com.hieu.catalog_service.application.common.CommandHandler;
import com.hieu.catalog_service.domain.exception.AttrNotFoundException;
import com.hieu.catalog_service.domain.model.attribute.valueobject.AttrId;
import com.hieu.catalog_service.domain.repository.AttrRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RemoveAttrValueHandler implements CommandHandler<RemoveAttrValueCommand, Void> {

    private final AttrRepository attrRepository;

    @Override
    public Void handle(RemoveAttrValueCommand cmd) {
        var attr = attrRepository.findById(AttrId.of(cmd.attrId()))
            .orElseThrow(() -> new AttrNotFoundException(cmd.attrId()));
        attr.removeValue(cmd.valId());
        attrRepository.save(attr);
        return null;
    }
}
