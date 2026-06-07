package com.hieu.auth_service.domain.models.role.events;

import com.hieu.auth_service.domain.events.DomainEvent;

public final class PermissionGrantedEvent extends DomainEvent {
    private final String roleId;
    private final String permissionId;

    public PermissionGrantedEvent(String roleId, String permissionId) {
        this.roleId = roleId;
        this.permissionId = permissionId;
    }

    @Override
    public String aggregateId() { return roleId; }

    public String roleId()       { return roleId; }
    public String permissionId() { return permissionId; }
}
