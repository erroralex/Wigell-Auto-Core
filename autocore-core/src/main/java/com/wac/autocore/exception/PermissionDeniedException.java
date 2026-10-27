package com.wac.autocore.exception;

import com.wac.autocore.security.Permission;
import com.wac.autocore.security.Role;

/**
 * <b>PermissionDeniedException</b>
 * <p>Responsibility: Thrown when a role lacks the permission for an operation, outside Spring Security.</p>
 * <p>Uses the same language keys as {@code SecurityErrorHandler}, so a denial in standalone shows
 * exactly the same text as an {@code AccessDeniedException} in the GUI.</p>
 */
public class PermissionDeniedException extends DomainException {

    public PermissionDeniedException(Role role, Permission permission) {
        super(
                "Role " + role + " lacks permission " + permission,
                "error.title",
                "error.accessDenied"
        );
    }
}
