package com.hieu.catalog_service.application.handler.attr;

import com.hieu.catalog_service.application.common.QueryHandler;
import com.hieu.catalog_service.application.dto.AttrDTO;
import com.hieu.catalog_service.application.mapper.CatalogDtoMapper;
import com.hieu.catalog_service.application.query.attr.ListAttrsQuery;
import com.hieu.catalog_service.domain.repository.AttrRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListAttrsHandler implements QueryHandler<ListAttrsQuery, List<AttrDTO>> {

    private final AttrRepository attrRepository;
    private final CatalogDtoMapper mapper;

    @Override
    public List<AttrDTO> handle(ListAttrsQuery query) {
        return attrRepository.findAllWithValues().stream().map(mapper::toDto).toList();
    }
}
