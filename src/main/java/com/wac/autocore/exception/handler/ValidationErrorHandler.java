package com.wac.autocore.exception.handler;

import javax.validation.ConstraintViolationException;

import javax.validation.ConstraintViolation;
import java.util.Comparator;

/**
 * Handles Bean Validation failures and preserves their message templates as i18n keys.
 */
public class ValidationErrorHandler extends ErrorHandler {
    @Override
    protected boolean canHandle(Throwable error) {
        return findCause(error, ConstraintViolationException.class) != null;
    }

    @Override
    protected ErrorReport createReport(Throwable error) {
        ConstraintViolationException validation = findCause(error, ConstraintViolationException.class);
        ConstraintViolation<?> violation = validation.getConstraintViolations().stream()
                .min(Comparator.comparing(v -> v.getPropertyPath().toString()))
                .orElseThrow(() -> new IllegalStateException(
                        "ConstraintViolationException contained no violations"
                ));

        String messageKey = violation.getMessageTemplate();
        if (messageKey != null && messageKey.startsWith("{") && messageKey.endsWith("}")) {
            messageKey = messageKey.substring(1, messageKey.length() - 1);
        }
        if (messageKey != null && messageKey.startsWith("javax.validation.constraints")) {
            messageKey = "error.fields";
        }

        return new ErrorReport(
                ErrorReport.Severity.EXPECTED,
                "error.validation",
                messageKey,
                null,
                validation
        );
    }
}
