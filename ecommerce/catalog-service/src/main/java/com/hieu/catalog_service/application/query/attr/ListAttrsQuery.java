package com.hieu.catalog_service.application.query.attr;

import com.hieu.catalog_service.application.common.Query;
import com.hieu.catalog_service.application.dto.AttrDTO;

import java.util.List;

public record ListAttrsQuery() implements Query<List<AttrDTO>> {}
