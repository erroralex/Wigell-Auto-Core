package com.wac.autocore.view.util;

import com.wac.autocore.exception.handler.ErrorHandler;
import com.wac.autocore.exception.handler.ErrorHandlerChain;
import com.wac.autocore.exception.handler.ErrorReport;
import com.wac.autocore.i18n.ErrorReportFormatter;
import com.wac.autocore.i18n.LocalizedError;
import com.wac.autocore.logging.CorrelationIds;
import com.wac.autocore.logging.LogKeys;
import com.wac.autocore.view.i18n.LanguageManager;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * <b>ErrorFacade</b>
 * <p>Responsibility: A single entry point (<b>Facade</b>) for showing errors in the UI. The text is translated by {@code ErrorReportFormatter},
 * the same as in headless mode, so the errors look the same in both.</p>
 * <p>Errors that are not caught at all end up here automatically via {@link GlobalExceptionHandler}.</p>
 * <p>Unexpected errors never show internal details. The user sees a general text and a reference
 * (the operation's correlation id, see {@link com.wac.autocore.logging.CorrelationIds}). The same id
 * is put in the MDC when the error is logged, so the reference leads straight to the stack trace.</p>
 * <p><b>Example usage in a view:</b></p>
 * <pre>{@code
 * try {
 *     workOrderService.createWorkOrder(booking.getId());
 * } catch (RuntimeException e) {
 *     ErrorFacade.handle(e);
 * }
 * }</pre>
 */
public final class ErrorFacade {

    private static final Logger log = LoggerFactory.getLogger(ErrorFacade.class);
    private static final ErrorHandler CHAIN = ErrorHandlerChain.createDefault();

    private static final ErrorReportFormatter FORMATTER =
            new ErrorReportFormatter(LanguageManager.getInstance());

    private static boolean presenting;

    private ErrorFacade() {
    }

    // Logs the error and shows it to the user.
    public static void handle(Throwable error) {
        if (error == null) {
            return;
        }

        final ErrorReport report = CHAIN.resolve(error);
        final String reference = resolveReference(error);
        logReport(report, reference);

        if (Platform.isFxApplicationThread()) {
            present(report, reference);
            return;
        }

        try {
            Platform.runLater(() -> present(report, reference));
        } catch (IllegalStateException toolkitNotRunning) {
            log.error("Could not show error alert, JavaFX not running", toolkitNotRunning);
        }
    }

    // Picks the id the user will see: the failed operation's id, else the current one, else a new one.
    private static String resolveReference(Throwable error) {
        String id = CorrelationIds.find(error);
        if (id == null) {
            id = MDC.get(LogKeys.CORR_ID);
        }
        if (id == null) {
            id = CorrelationIds.newId();
        }
        return id;
    }

    // Translates the report and shows the right kind of dialog. Always runs on the JavaFX thread.
    private static void present(ErrorReport report, String reference) {
        if (presenting) {
            log.warn("Error dialog already showing, new error only logged: {}", report.getMessageKey());
            return;
        }
        presenting = true;
        try {
            LocalizedError error = FORMATTER.format(report);

            if (report.isUnexpected()) {
                AlertHelper.showUnexpected(error.getTitle(), error.getMessage(),
                        LanguageManager.getInstance().get("error.reference"), reference);
            } else {
                AlertHelper.showError(error.getTitle(), error.getMessage());
            }
        } catch (RuntimeException presentationError) {
            log.error("Could not show error alert", presentationError);
        } finally {
            presenting = false;
        }
    }

    // Logs at different levels depending on severity, with the reference as corrId so the log line can be found.
    private static void logReport(ErrorReport report, String reference) {
        String previous = MDC.get(LogKeys.CORR_ID);
        MDC.put(LogKeys.CORR_ID, reference);
        try {
            if (report.isUnexpected()) {
                log.error("Unexpected error ({})", report.getMessageKey(), report.getCause());
            } else {
                log.info("Business error ({}): {}", report.getMessageKey(),
                        report.getCause() == null ? "null" : report.getCause().getMessage());
            }
        } finally {
            if (previous == null) {
                MDC.remove(LogKeys.CORR_ID);
            } else {
                MDC.put(LogKeys.CORR_ID, previous);
            }
        }
    }
}
