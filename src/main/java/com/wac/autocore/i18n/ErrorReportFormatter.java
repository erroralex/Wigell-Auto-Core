package com.wac.autocore.i18n;

import com.wac.autocore.exception.handler.ErrorReport;

import java.util.Objects;

/**
 * <b>ErrorReportFormatter</b>
 * <p>Ansvar: Det enda stället där en {@link ErrorReport} blir text. Både ErrorFacade och headless använder klassen.</p>
 */
public final class ErrorReportFormatter {

    private final Translator translator;

    public ErrorReportFormatter(Translator translator) {
        this.translator = Objects.requireNonNull(translator, "translator");
    }

    public LocalizedError format(ErrorReport report) {
        Objects.requireNonNull(report, "report");
        String title = translator.get(report.getTitleKey());
        String message = translator.get(report.getMessageKey(), report.getMessageArgs());
        return new LocalizedError(title, message);
    }
}
