package com.wac.autocore.exception.handler;

import org.springframework.dao.DataAccessException;

/**
 * <b>DataAccessErrorHandler</b>
 * <p>Ansvar: Andra ledet i felkedjan. Hanterar databasfel från Spring Data, t.ex. när
 * SQLite-filen är låst av en annan process eller saknas.</p>
 * <p>Spring översätter JDBC- och Hibernate-fel till den gemensamma hierarkin
 * {@link DataAccessException}. Därför räcker det att leta efter den typen, oavsett vilken
 * databas eller vilket ORM-ramverk som ligger under.</p>
 * <p>Felet räknas som oväntat: användaren får ett begripligt meddelande, men den tekniska
 * detaljen visas också eftersom den behövs för felsökning.</p>
 */
public class DataAccessErrorHandler extends ErrorHandler {

    // Svarar ja om det finns ett DataAccessException någonstans i orsakskedjan
    @Override
    protected boolean canHandle(Throwable error) {
        return findCause(error, DataAccessException.class) != null;
    }

    @Override
    protected ErrorReport createReport(Throwable error) {
        return new ErrorReport(
                ErrorReport.Severity.UNEXPECTED,
                "error.title",
                "error.database",
                null,
                error
        );
    }
}
