package com.hieu.catalog_service.infrastructure.persistence.impl;

import com.hieu.catalog_service.domain.model.attribute.Attr;
import com.hieu.catalog_service.domain.model.attribute.AttrVal;
import com.hieu.catalog_service.domain.model.attribute.valueobject.AttrId;
import com.hieu.catalog_service.domain.repository.AttrRepository;
import com.hieu.catalog_service.infrastructure.persistence.jpa.entities.AttrJpaEntity;
import com.hieu.catalog_service.infrastructure.persistence.jpa.repositories.AttrJpaRepository;
import com.hieu.catalog_service.infrastructure.persistence.mapper.AttrJpaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AttrRepositoryImpl implements AttrRepository {

    private final AttrJpaRepository jpa;
    private final AttrJpaMapper mapper;

    @Override
    public Attr save(Attr attr) {
        AttrJpaEntity existing = attr.getId() != null
            ? jpa.findByIdWithValues(attr.getId().value()).orElse(null)
            : null;
        AttrJpaEntity saved = jpa.save(mapper.toJpa(attr, existing));
        if (attr.getId() == null) attr.assignId(AttrId.of(saved.getId()));
        // Propagate generated value ids back onto the domain's AttrVals (match by code).
        var idByCode = new HashMap<String, String>();
        saved.getValues().forEach(v -> idByCode.put(v.getCode(), v.getId()));
        for (AttrVal v : attr.getValues()) {
            if (v.getId() == null) {
                String id = idByCode.get(v.getCode());
                if (id != null) v.assignId(id);
            }
        }
        return attr;
    }

    @Override
    public Optional<Attr> findById(AttrId id) {
        return jpa.findByIdWithValues(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<Attr> findByCode(String code) {
        return jpa.findByCodeWithValues(code).map(mapper::toDomain);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpa.existsByCode(code);
    }

    @Override
    public List<Attr> findAllWithValues() {
        return jpa.findAllWithValues().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Attr> findAllByIdsWithValues(List<String> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return jpa.findAllByIdsWithValues(ids).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Attr attr) {
        if (attr.getId() != null) jpa.deleteById(attr.getId().value());
    }
}
