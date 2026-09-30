package com.wac.autocore.exception.handler;

/**
 * <b>FallbackErrorHandler</b>
 * <p>Ansvar: Sista ledet i felkedjan. Tar hand om alla fel som inga tidigare led känner igen,
 * t.ex. {@code NullPointerException} eller {@code IllegalStateException}, och visar dem som
 * oväntade fel med tekniska detaljer.</p>
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
