package com.wac.autocore.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessEvaluatorTest {

    private final AccessEvaluator access = new AccessEvaluator();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void login(String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("user", "pw",
                        AuthorityUtils.createAuthorityList(authorities)));
    }

    @Test
    void deniesWhenNobodyIsLoggedIn() {
        assertFalse(access.can("DATA_READ"));
    }

    @Test
    void deniesAnonymousUser() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anon", AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
        assertFalse(access.can("DATA_READ"));
    }

    @Test
    void deniesUnknownRole() {
        login("ROLE_HACKER");
        assertFalse(access.can("DATA_READ"));
    }

    @Test
    void deniesAuthorityWithoutRolePrefix() {
        login("ADMIN");
        assertFalse(access.can("DATA_READ"));
    }

    @Test
    void deniesUnknownOrNullPermission() {
        login("ROLE_ADMIN");
        assertFalse(access.can("NOT_A_PERMISSION"));
        assertFalse(access.can(null));
    }

    @Test
    void adminHasEveryPermission() {
        login("ROLE_ADMIN");
        for (Permission p : Permission.values()) {
            assertTrue(access.can(p.name()), p.name());
        }
    }

    @Test
    void workshopFollowsThePolicy() {
        login("ROLE_WORKSHOP");
        assertTrue(access.can("WORK_ORDER_WRITE"));
        assertFalse(access.can("INVOICE_CREATE"));
        assertFalse(access.can("PAYMENT_REGISTER"));
        assertFalse(access.can("CUSTOMER_DELETE"));
    }

    @Test
    void readOnlyCanOnlyRead() {
        login("ROLE_READ_ONLY");
        assertTrue(access.can("DATA_READ"));
        for (Permission p : Permission.values()) {
            if (p != Permission.DATA_READ) {
                assertFalse(access.can(p.name()), p.name());
            }
        }
    }
}