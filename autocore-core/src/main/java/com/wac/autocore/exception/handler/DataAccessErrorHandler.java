package com.wac.autocore.exception.handler;

import org.springframework.dao.DataAccessException;

/**
 * <b>DataAccessErrorHandler</b>
 * <p>Responsibility: Second link in the error chain. Handles database errors from Spring Data, e.g. when
 * the SQLite file is locked by another process or is missing.</p>
 * <p>Spring translates JDBC and Hibernate errors into the common hierarchy
 * {@link DataAccessException}. That is why it is enough to look for that type, regardless of which
 * database or ORM framework is underneath.</p>
 * <p>The error counts as unexpected: the user gets an understandable message, but the technical
 * detail is also shown because it is needed for troubleshooting.</p>
 */
public class DataAccessErrorHandler extends ErrorHandler {

    // Answers yes if there is a DataAccessException anywhere in the cause chain
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
