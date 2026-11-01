package com.wac.autocore.security;

import com.wac.autocore.AutoCoreConfig;
import com.wac.autocore.exception.handler.ErrorHandlerChain;
import com.wac.autocore.service.CustomerService;
import com.wac.autocore.service.WorkOrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * <b>UnauthenticatedAccessSecurityTest</b>
 * <p>Responsibility: Proves that protected service methods cannot be reached around the UI. A call with no
 * authentication at all gives {@link AuthenticationCredentialsNotFoundException}, an anonymous identity gives
 * {@link AccessDeniedException}, and the two map to different error keys.</p>
 * <p>The headless runner without credentials is covered by {@code HeadlessRunnerTest}.</p>
 */
@SpringBootTest(classes = AutoCoreConfig.class)
@ActiveProfiles("test")
@DirtiesContext
class UnauthenticatedAccessSecurityTest {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void sqliteFile(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:sqlite:" + tempDir.resolve("unauthenticated-security.db").toAbsolutePath());
    }

    @Autowired private WorkOrderService workOrders;
    @Autowired private CustomerService customers;

    // The security context is thread-local, so no identity may leak on to another test.
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void noAuthenticationAtAllIsNotAuthenticated() {
        SecurityContextHolder.clearContext();

        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> workOrders.findAll());
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> customers.findAll());
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> customers.deleteById(1));

        Exception error = assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> workOrders.completeWorkOrder(1));
        assertEquals("error.notAuthenticated", ErrorHandlerChain.createDefault().resolve(error).getMessageKey());
    }

    @Test
    @WithAnonymousUser
    void anonymousIdentityIsAccessDenied() {
        assertThrows(AccessDeniedException.class, () -> customers.deleteById(1));

        Exception error = assertThrows(AccessDeniedException.class, () -> workOrders.completeWorkOrder(1));
        assertEquals("error.accessDenied", ErrorHandlerChain.createDefault().resolve(error).getMessageKey());
    }
}
