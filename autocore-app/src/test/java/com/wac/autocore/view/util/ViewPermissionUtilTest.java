package com.wac.autocore.view.util;

import com.wac.autocore.security.Permission;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Plain unit test: no Spring context and no JavaFX toolkit. Region is a node that can be created without a running UI.
class ViewPermissionUtilTest {

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void login(String role) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("user", "pw",
                        AuthorityUtils.createAuthorityList("ROLE_" + role)));
    }

    @Test
    void nobodyLoggedInCanDoNothing() {
        for (Permission p : Permission.values()) {
            assertFalse(ViewPermissionUtil.can(p), p.name());
        }
    }

    @Test
    void adminCanDoEverything() {
        login("ADMIN");
        for (Permission p : Permission.values()) {
            assertTrue(ViewPermissionUtil.can(p), p.name());
        }
    }

    @Test
    void workshopCanWriteWorkOrdersButNotChangeTheCatalog() {
        login("WORKSHOP");
        assertTrue(ViewPermissionUtil.can(Permission.WORK_ORDER_WRITE));
        assertFalse(ViewPermissionUtil.can(Permission.SERVICE_CATALOG_WRITE));
    }

    @Test
    void readOnlyCanOnlyRead() {
        login("READ_ONLY");
        assertTrue(ViewPermissionUtil.can(Permission.DATA_READ));
        assertFalse(ViewPermissionUtil.can(Permission.WORK_ORDER_WRITE));
    }

    @Test
    void hideUnlessHidesAllNodesWhenPermissionIsMissing() {
        login("READ_ONLY");
        Region first = new Region();
        Region second = new Region();

        ViewPermissionUtil.hideUnless(Permission.WORK_ORDER_WRITE, first, second);

        for (Region node : new Region[]{first, second}) {
            assertFalse(node.isVisible());
            assertFalse(node.isManaged());
        }
    }

    @Test
    void hideUnlessKeepsNodesWhenPermissionIsHeld() {
        login("WORKSHOP");
        Region node = new Region();

        ViewPermissionUtil.hideUnless(Permission.WORK_ORDER_WRITE, node);

        assertTrue(node.isVisible());
        assertTrue(node.isManaged());
    }

    @Test
    void hideUnlessHidesWhenNobodyIsLoggedIn() {
        Region node = new Region();

        ViewPermissionUtil.hideUnless(Permission.DATA_READ, node);

        assertFalse(node.isVisible());
        assertFalse(node.isManaged());
    }
}
