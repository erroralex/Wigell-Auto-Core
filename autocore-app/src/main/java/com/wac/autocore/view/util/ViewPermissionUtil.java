package com.wac.autocore.view.util;

import com.wac.autocore.security.AccessEvaluator;
import com.wac.autocore.security.Permission;
import javafx.scene.Node;

// Lets the views ask whether the logged-in user holds a permission, and hide controls the user may not use.
// It complements the service rules from PreAuthorize, which stops a call that gets through anyway.
// Reuses AccessEvaluator, so the UI and the services apply the same rule.
// The evaluator holds no state and reads the current authentication each time,
// it is created with "new" here, because the views are not Spring beans.
public final class ViewPermissionUtil {

    private static final AccessEvaluator ACCESS = new AccessEvaluator();

    private ViewPermissionUtil() {
    }

    // True if the logged-in user holds the permission. False when nobody is logged in.
    public static boolean can(Permission permission) {
        return ACCESS.can(permission.name());
    }

    // Hides the nodes (invisible and without layout space) unless the user holds the permission.
    public static void hideUnless(Permission permission, Node... nodes) {
        boolean allowed = can(permission);
        for (Node node : nodes) {
            node.setVisible(allowed);
            node.setManaged(allowed);
        }
    }
}
