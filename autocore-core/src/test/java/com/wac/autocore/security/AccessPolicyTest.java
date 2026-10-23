package com.wac.autocore.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static com.wac.autocore.security.Permission.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AccessPolicy")
class AccessPolicyTest {

    private static Set<Permission> expectedFor(Role role) {
        switch (role) {
            case ADMIN:
                return EnumSet.of(
                        DATA_READ,
                        CUSTOMER_WRITE,
                        CUSTOMER_DELETE,
                        VEHICLE_WRITE,
                        VEHICLE_DELETE,
                        BOOKING_WRITE,
                        WORK_ORDER_WRITE,
                        INVOICE_CREATE,
                        PAYMENT_REGISTER,
                        SERVICE_CATALOG_WRITE,
                        MECHANIC_MANAGE
                );

            case WORKSHOP:
                return EnumSet.of(
                        DATA_READ,
                        CUSTOMER_WRITE,
                        VEHICLE_WRITE,
                        BOOKING_WRITE,
                        WORK_ORDER_WRITE
                );

            case READ_ONLY:
                return EnumSet.of(DATA_READ);

            default:
                throw new IllegalStateException("Missing in test: " + role);
        }
    }

    static Stream<Arguments> combinations() {
        List<Arguments> args = new ArrayList<>();

        for (Role role : Role.values()) {
            Set<Permission> expectedPermissions = expectedFor(role);

            for (Permission permission : Permission.values()) {
                args.add(Arguments.of(
                        role,
                        permission,
                        expectedPermissions.contains(permission)
                ));
            }
        }

        return args.stream();
    }

    @DisplayName("Should match the expected permission matrix for every role and permission")
    @ParameterizedTest(name = "{0} + {1} -> {2}")
    @MethodSource("combinations")
    void matchesMatrix(Role role, Permission permission, boolean expected) {
        assertEquals(expected, AccessPolicy.can(role, permission));
    }

    @Test
    @DisplayName("Should cover exactly 33 role-permission combinations")
    void coversAll33Combinations() {
        assertEquals(33, combinations().count());
    }

    @Test
    @DisplayName("Should apply identical write and delete permissions to customers and vehicles")
    void customerAndVehicleRulesAreEqual() {
        for (Role role : Role.values()) {
            assertEquals(
                    AccessPolicy.can(role, CUSTOMER_WRITE),
                    AccessPolicy.can(role, VEHICLE_WRITE),
                    "Customer and vehicle write permissions should match for " + role
            );

            assertEquals(
                    AccessPolicy.can(role, CUSTOMER_DELETE),
                    AccessPolicy.can(role, VEHICLE_DELETE),
                    "Customer and vehicle delete permissions should match for " + role
            );
        }
    }

    @Test
    @DisplayName("Should deny access when the role or permission is null")
    void nullsAreDenied() {
        assertFalse(AccessPolicy.can(null, DATA_READ));
        assertFalse(AccessPolicy.can(Role.ADMIN, null));
        assertFalse(AccessPolicy.can(null, null));
    }
}
