package com.wac.autocore.exception.handler;

import com.wac.autocore.exception.DomainException;

/**
 * <b>DomainErrorHandler</b>
 * <p>Responsibility: First link in the error chain. Handles all expected business errors, that is, all
 * subclasses of {@link DomainException}, e.g. double booking, locked booking or validation errors.</p>
 */
public class DomainErrorHandler extends ErrorHandler {

    // Answers yes if there is a DomainException anywhere in the cause chain
    @Override
    protected boolean canHandle(Throwable error) {
        return findCause(error, DomainException.class) != null;
    }

    @Override
    protected ErrorReport createReport(Throwable error) {
        DomainException domain = findCause(error, DomainException.class);
        return new ErrorReport(
                ErrorReport.Severity.EXPECTED,
                domain.getTitleKey(),
                domain.getMessageKey(),
                domain.getMessageArgs(),
                domain
        );
    }
}
