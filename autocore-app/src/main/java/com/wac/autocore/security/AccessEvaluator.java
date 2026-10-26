package com.wac.autocore.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

// Bridge between Spring Security and the framework-independent AccessPolicy.
// Used in rules such as @PreAuthorize("@access.can('WORK_ORDER_WRITE')").
// Reads the current authentication only, never a role passed in by the caller.
// Denies access when nobody is logged in, the user is anonymous, or the role or permission is unknown.
// Flow: AppUserDetailsService supplies ROLE_<role> authorities -> @PreAuthorize calls access.can(...)
// -> AccessEvaluator maps the role and permission -> AccessPolicy decides.
@Component("access")
public class AccessEvaluator {
    public boolean can(String permission) {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated() || a instanceof AnonymousAuthenticationToken) return false;
        Permission p;
        try { p = Permission.valueOf(permission); } catch (IllegalArgumentException | NullPointerException e) { return false; }
        for (GrantedAuthority g : a.getAuthorities()) {
            String name = g.getAuthority();
            if (name != null && name.startsWith("ROLE_")) {
                try {
                    if (AccessPolicy.can(Role.valueOf(name.substring(5)), p)) return true;
                } catch (IllegalArgumentException ignored) { }
            }
        }
        return false;
    }
}
