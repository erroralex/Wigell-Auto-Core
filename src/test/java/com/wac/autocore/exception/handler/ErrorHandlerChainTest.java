package com.wac.autocore.exception.handler;

import com.wac.autocore.exception.IllegalStatusTransitionException;
import com.wac.autocore.model.WorkOrderStatus;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
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

    private ConstraintViolation<?> violation(String property, String messageTemplate) {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn(property);
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessageTemplate()).thenReturn(messageTemplate);
        return violation;
    }


}
