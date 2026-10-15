package com.wac.autocore.exception.handler;

import com.wac.autocore.service.LanguageManager;

import javax.validation.ConstraintViolationException;

import javax.validation.ConstraintViolation;

public class ValidationErrorHandler extends ErrorHandler {
    @Override
    protected boolean canHandle(Throwable error) {
        return findCause(error, ConstraintViolationException.class) != null;
    }

    @Override
    protected ErrorReport createReport(Throwable error) {
        ConstraintViolationException validation = findCause(error, ConstraintViolationException.class);
        ConstraintViolation<?> violation = validation.getConstraintViolations().iterator().next();

        String messageKey = violation.getMessageTemplate();
        if (messageKey != null && messageKey.startsWith("{") && messageKey.endsWith("}")) {
            messageKey = messageKey.substring(1, messageKey.length() - 1);
        }
        if (messageKey != null && messageKey.startsWith("javax.validation.constraints")) {
            messageKey = "error.fields";
        }

        Object[] messageArgs = null;
        if ("error.requiredField".equals(messageKey)) {
            String path = violation.getPropertyPath().toString();
            String field = path.substring(path.lastIndexOf('.') + 1);
            messageArgs = new Object[] {
                    Translator.getInstance().get("table." + field)
            };
        }

        return new ErrorReport(
                ErrorReport.Severity.EXPECTED,
                "error.validation",
                messageKey,
                messageArgs,
                validation
        );
    }
}
