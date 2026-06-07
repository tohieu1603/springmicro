package com.hieu.catalog_service.application.command.attr;

import com.hieu.catalog_service.application.common.Command;
import com.hieu.catalog_service.application.dto.AttrDTO;

public record AddAttrValueCommand(String attrId, String val, String code) implements Command<AttrDTO> {}
