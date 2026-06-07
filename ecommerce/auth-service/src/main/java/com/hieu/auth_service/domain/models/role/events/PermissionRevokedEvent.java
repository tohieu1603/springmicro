package com.hieu.auth_service.domain.models.role.events;

import com.hieu.auth_service.domain.events.DomainEvent;

public final class PermissionRevokedEvent extends DomainEvent {
    private final String roleId;
    private final String permissionId;

    public PermissionRevokedEvent(String roleId, String permissionId) {
        this.roleId = roleId;
        this.permissionId = permissionId;
    }

    @Override public String aggregateId() { return roleId; }

    public String roleId()       { return roleId; }
    public String permissionId() { return permissionId; }
}
