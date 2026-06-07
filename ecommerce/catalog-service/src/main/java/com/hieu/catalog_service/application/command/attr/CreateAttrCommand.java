package com.hieu.catalog_service.application.command.attr;

import com.hieu.catalog_service.application.common.Command;
import com.hieu.catalog_service.application.dto.AttrDTO;

import java.util.List;

public record CreateAttrCommand(
        String code,
        String name,
        String type,
        List<ValueCmd> values
) implements Command<AttrDTO> {

    public record ValueCmd(String val, String code) {}
}
