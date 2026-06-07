package com.hieu.catalog_service.application.query.attr;

import com.hieu.catalog_service.application.common.Query;
import com.hieu.catalog_service.application.dto.AttrDTO;

public record GetAttrByIdQuery(String attrId) implements Query<AttrDTO> {}
