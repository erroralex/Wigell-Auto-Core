package com.wac.autocore.exception.handler;

import com.wac.autocore.exception.DomainException;

/**
 * <b>DomainErrorHandler</b>
 * <p>Ansvar: Första ledet i felkedjan. Hanterar alla förväntade affärsfel, det vill säga alla
 * subklasser till {@link DomainException}, t.ex. dubbelbokning, låst bokning eller valideringsfel.</p>
 */
public class DomainErrorHandler extends ErrorHandler {

    // Svarar ja om det finns ett DomainException någonstans i orsakskedjan
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
