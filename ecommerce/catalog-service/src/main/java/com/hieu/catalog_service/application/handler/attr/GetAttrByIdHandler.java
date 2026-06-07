package com.hieu.catalog_service.application.handler.attr;

import com.hieu.catalog_service.application.common.QueryHandler;
import com.hieu.catalog_service.application.dto.AttrDTO;
import com.hieu.catalog_service.application.mapper.CatalogDtoMapper;
import com.hieu.catalog_service.application.query.attr.GetAttrByIdQuery;
import com.hieu.catalog_service.domain.exception.AttrNotFoundException;
import com.hieu.catalog_service.domain.model.attribute.valueobject.AttrId;
import com.hieu.catalog_service.domain.repository.AttrRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetAttrByIdHandler implements QueryHandler<GetAttrByIdQuery, AttrDTO> {

    private final AttrRepository attrRepository;
    private final CatalogDtoMapper mapper;

    @Override
    public AttrDTO handle(GetAttrByIdQuery query) {
        return attrRepository.findById(AttrId.of(query.attrId()))
            .map(mapper::toDto)
            .orElseThrow(() -> new AttrNotFoundException(query.attrId()));
    }
}
