package com.wac.autocore.security;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

// AccessPolicy maps roles to permissions, defining what actions each role is allowed to perform in the system.
// Spring Security can use this class as an argument to enforce access control based on the user's role and the required permission for a given action.
// The services-layer can use it to make sure that protected operations are only accessible to users with the appropriate permissions, as defined in this access policy.
// This class can be used by both autocore-app and autocore-standalone by calling AccessPolicy.can().
public final class AccessPolicy {

    // The matrix is made only once and is immutable, ensuring that the access policy cannot be changed at runtime.
    private static final Map<Role, Set<Permission>> PERMISSIONS_BY_ROLE =
            createPermissionMatrix();

    private AccessPolicy() {
        // Utility class, instances are not needed.
    }

    // Takes a role and a permission and returns true if the role has the permission, false otherwise.
    // Returns false if either the role or permission is null, meaning a missing value always results in a denial of access.
    public static boolean can(Role role, Permission permission) {
        if (role == null || permission == null) {
            return false;
        }

        return PERMISSIONS_BY_ROLE
                .getOrDefault(role, Collections.emptySet())
                .contains(permission);
    }

    // Enum Map where Role = Key and Permission = Value.
    // Wrapping each map with "Collections.unmodifiableMap" to ensure immutability.
    private static Map<Role, Set<Permission>> createPermissionMatrix() {
        Map<Role, Set<Permission>> matrix = new EnumMap<>(Role.class);

        // Admin has all permissions, even if new permissions are added in the future.
        matrix.put(Role.ADMIN, Collections.unmodifiableSet(EnumSet.allOf(Permission.class)));

        // Workshop has specific permissions
        matrix.put(Role.WORKSHOP, Collections.unmodifiableSet(EnumSet.of(
                Permission.DATA_READ,
                Permission.CUSTOMER_WRITE,
                Permission.VEHICLE_WRITE,
                Permission.BOOKING_WRITE,
                Permission.WORK_ORDER_WRITE
        )));

        // Read-only has limited permissions
        matrix.put(Role.READ_ONLY, Collections.unmodifiableSet(EnumSet.of(
                Permission.DATA_READ
        )));

        // Returns the matching permissions for each role as an unmodifiable map.
        return Collections.unmodifiableMap(matrix);
    }
}
