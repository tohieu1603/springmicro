package com.hieu.catalog_service.infrastructure.persistence.mapper;

import com.hieu.catalog_service.domain.model.attribute.Attr;
import com.hieu.catalog_service.domain.model.attribute.AttrVal;
import com.hieu.catalog_service.domain.model.attribute.valueobject.AttrId;
import com.hieu.catalog_service.domain.model.attribute.valueobject.AttrType;
import com.hieu.catalog_service.infrastructure.persistence.jpa.entities.AttrJpaEntity;
import com.hieu.catalog_service.infrastructure.persistence.jpa.entities.AttrValJpaEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Domain ↔ JPA for Attr aggregate. Reconciles child AttrVals in-place so Hibernate
 *  sees adds/updates/removes as {@code @OneToMany(orphanRemoval=true)} events. */
@Component
public class AttrJpaMapper {

    public AttrJpaEntity toJpa(Attr a, AttrJpaEntity existing) {
        AttrJpaEntity e = existing != null ? existing : new AttrJpaEntity();
        e.setCode(a.getCode());
        e.setName(a.getName());
        e.setType(a.getType().name());
        e.setSortOrder(a.getSortOrder());

        Map<String, AttrValJpaEntity> byId = new HashMap<>();
        e.getValues().forEach(v -> { if (v.getId() != null) byId.put(v.getId(), v); });

        // Compute the target set from the domain; match by id when present, else insert.
        var targetEntities = new java.util.ArrayList<AttrValJpaEntity>();
        for (AttrVal val : a.getValues()) {
            AttrValJpaEntity ve = val.getId() != null ? byId.get(val.getId()) : null;
            if (ve == null) {
                ve = new AttrValJpaEntity();
                ve.setAttr(e);
            }
            ve.setVal(val.getVal());
            ve.setCode(val.getCode());
            ve.setSortOrder(val.getSortOrder());
            targetEntities.add(ve);
        }
        // In-place mutation keeps the JPA-managed collection identity.
        e.getValues().clear();
        e.getValues().addAll(targetEntities);
        return e;
    }

    public Attr toDomain(AttrJpaEntity e) {
        var values = e.getValues().stream()
            .map(v -> AttrVal.reconstitute(v.getId(),
                Objects.requireNonNull(e.getId(), "attr id"),
                v.getVal(), v.getCode(), v.getSortOrder()))
            .toList();
        return Attr.reconstitute(
            AttrId.of(e.getId()), e.getCode(), e.getName(),
            AttrType.fromString(e.getType()), e.getSortOrder(), values);
    }
}
