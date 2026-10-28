package com.wac.autocore.exception.handler;

import com.wac.autocore.exception.IllegalStatusTransitionException;
import com.wac.autocore.exception.PermissionDeniedException;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.security.Permission;
import com.wac.autocore.security.Role;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationDeniedException;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ErrorHandlerChainTest {

    @Test
    void resolvesDomainException() {
        IllegalStatusTransitionException error = new IllegalStatusTransitionException(
                WorkOrderStatus.CANCELLED, WorkOrderStatus.IN_PROGRESS);

        ErrorReport report = ErrorHandlerChain.createDefault().resolve(error);

        assertEquals(ErrorReport.Severity.EXPECTED, report.getSeverity());
        assertEquals("error.statusTransition", report.getTitleKey());
        assertEquals("error.statusTransitionMsg", report.getMessageKey());
        assertSame(error, report.getCause());
    }

    @Test
    void resolvesConstraintViolationException() {
        ConstraintViolation<?> laterViolation = violation("zField", "error.desc");
        ConstraintViolation<?> firstViolation = violation("aField", "error.dateSelect");
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        violations.add(laterViolation);
        violations.add(firstViolation);
        ConstraintViolationException error =
                new ConstraintViolationException(violations);

        ErrorReport report = ErrorHandlerChain.createDefault().resolve(error);

        assertEquals(ErrorReport.Severity.EXPECTED, report.getSeverity());
        assertEquals("error.validation", report.getTitleKey());
        assertEquals("error.dateSelect", report.getMessageKey());
        assertSame(error, report.getCause());
    }

    @Test
    void resolvesNotAuthenticated() {
        AuthenticationCredentialsNotFoundException error =
                new AuthenticationCredentialsNotFoundException("no authentication");

        ErrorReport report = ErrorHandlerChain.createDefault().resolve(error);

        assertEquals(ErrorReport.Severity.EXPECTED, report.getSeverity());
        assertEquals("error.title", report.getTitleKey());
        assertEquals("error.notAuthenticated", report.getMessageKey());
        assertSame(error, report.getCause());
    }

    @Test
    void resolvesAccessDenied() {
        AccessDeniedException error = new AccessDeniedException("denied");

        ErrorReport report = ErrorHandlerChain.createDefault().resolve(error);

        assertEquals(ErrorReport.Severity.EXPECTED, report.getSeverity());
        assertEquals("error.title", report.getTitleKey());
        assertEquals("error.accessDenied", report.getMessageKey());
        assertSame(error, report.getCause());
    }

    @Test
    void resolvesAuthorizationDeniedAsAccessDenied() {
        AuthorizationDeniedException error =
                new AuthorizationDeniedException("denied", new AuthorizationDecision(false));

        ErrorReport report = ErrorHandlerChain.createDefault().resolve(error);

        assertEquals(ErrorReport.Severity.EXPECTED, report.getSeverity());
        assertEquals("error.accessDenied", report.getMessageKey());
        assertSame(error, report.getCause());
    }

    @Test
    void securityErrorInsideWrapperIsStillResolved() {
        AccessDeniedException cause = new AccessDeniedException("denied");

        ErrorReport report = ErrorHandlerChain.createDefault().resolve(new RuntimeException("wrapped", cause));

        assertEquals("error.accessDenied", report.getMessageKey());
        assertSame(cause, report.getCause());
    }

    private ConstraintViolation<?> violation(String property, String messageTemplate) {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn(property);
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessageTemplate()).thenReturn(messageTemplate);
        return violation;
    }

    @Test
    @DisplayName("Permission denied without Spring gives the same text as Spring's access denied")
    void permissionDeniedMatchesSpringAccessDenied() {
        ErrorHandler chain = ErrorHandlerChain.createDefault();
        ErrorReport fromProxy = chain.resolve(
                new PermissionDeniedException(Role.READ_ONLY, Permission.WORK_ORDER_WRITE));
        ErrorReport fromSpring = chain.resolve(new AccessDeniedException("denied"));

        assertEquals(fromSpring.getTitleKey(), fromProxy.getTitleKey());
        assertEquals(fromSpring.getMessageKey(), fromProxy.getMessageKey());
        assertFalse(fromProxy.isUnexpected());
    }
}
