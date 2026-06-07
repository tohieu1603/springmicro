package com.hieu.catalog_service.domain.repository;

import com.hieu.catalog_service.domain.model.attribute.Attr;
import com.hieu.catalog_service.domain.model.attribute.valueobject.AttrId;

import java.util.List;
import java.util.Optional;

public interface AttrRepository {

    Attr save(Attr attr);

    Optional<Attr> findById(AttrId id);

    Optional<Attr> findByCode(String code);

    boolean existsByCode(String code);

    /** All attributes (with their predefined values via JOIN FETCH). */
    List<Attr> findAllWithValues();

    /** Batch fetch — replaces N round-trips through {@link #findById} during variant ingestion. */
    List<Attr> findAllByIdsWithValues(List<String> ids);

    void delete(Attr attr);
}
