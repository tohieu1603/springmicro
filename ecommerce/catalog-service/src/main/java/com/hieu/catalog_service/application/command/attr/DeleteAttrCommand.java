package com.hieu.catalog_service.application.command.attr;

import com.hieu.catalog_service.application.common.Command;

public record DeleteAttrCommand(String attrId) implements Command<Void> {}
