package com.wac.autocore.exception;

import com.wac.autocore.security.Permission;
import com.wac.autocore.security.Role;

public class PermissionDeniedException extends DomainException {

    public PermissionDeniedException(Role role, Permission permission) {
        super(
                "Role " + role + " lacks permission " + permission,
                "error.accessDenied",
                "error.accessDeniedMsg",
                role,
                permission
        );
    }
}
