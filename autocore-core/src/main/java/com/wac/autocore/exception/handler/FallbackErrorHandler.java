package com.wac.autocore.exception.handler;

/**
 * <b>FallbackErrorHandler</b>
 * <p>Responsibility: Last link in the error chain. Takes care of all errors that no earlier link recognizes,
 * e.g. {@code NullPointerException} or {@code IllegalStateException}, and reports them as
 * unexpected errors. The user sees a general text and a reference id; the technical details
 * only go to the log.</p>
 */
public class FallbackErrorHandler extends ErrorHandler {

    @Override
    protected boolean canHandle(Throwable error) {
        return true;
    }

    @Override
    protected ErrorReport createReport(Throwable error) {
        return ErrorReport.unexpected(error);
    }
}
