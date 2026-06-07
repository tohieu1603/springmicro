package com.hieu.catalog_service.application.command.attr;

import com.hieu.catalog_service.application.common.Command;

public record RemoveAttrValueCommand(String attrId, String valId) implements Command<Void> {}
