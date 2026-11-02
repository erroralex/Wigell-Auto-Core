package com.wac.autocore.testsupport;

import org.springframework.security.test.context.support.WithMockUser;

import java.lang.annotation.*;

// Runs a test as an authenticated ADMIN, so service calls pass method security.
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@WithMockUser(username = "admin", roles = "ADMIN")
public @interface WithAdmin {
}