package com.wac.autocore.service.errorChain;

import com.wac.autocore.exception.IllegalStatusTransitionException;
import com.wac.autocore.exception.handler.ErrorHandlerChain;
import com.wac.autocore.exception.handler.ErrorReport;
import com.wac.autocore.model.WorkOrderStatus;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import java.util.Collections;

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
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getMessageTemplate()).thenReturn("error.dateSelect");
        ConstraintViolationException error =
                new ConstraintViolationException(Collections.singleton(violation));

        ErrorReport report = ErrorHandlerChain.createDefault().resolve(error);

        assertEquals(ErrorReport.Severity.EXPECTED, report.getSeverity());
        assertEquals("error.validation", report.getTitleKey());
        assertEquals("error.dateSelect", report.getMessageKey());
        assertSame(error, report.getCause());
    }


}
