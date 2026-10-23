package com.wac.autocore.view.util;

import com.wac.autocore.exception.handler.ErrorHandler;
import com.wac.autocore.exception.handler.ErrorHandlerChain;
import com.wac.autocore.exception.handler.ErrorReport;
import com.wac.autocore.i18n.ErrorReportFormatter;
import com.wac.autocore.i18n.LocalizedError;
import com.wac.autocore.view.i18n.LanguageManager;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <b>ErrorFacade</b>
 * <p>Responsibility: A single entry point (<b>Facade</b>) for showing errors in the UI. The text is translated by {@code ErrorReportFormatter},
 * the same as in headless mode, so the errors look the same in both.</p>
 * <p>Errors that are not caught at all end up here automatically via {@link GlobalExceptionHandler}.</p>
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
        logReport(report);

        if (Platform.isFxApplicationThread()) {
            present(report);
            return;
        }

        try {
            Platform.runLater(() -> present(report));
        } catch (IllegalStateException toolkitNotRunning) {
            log.error("Could not show error alert, JavaFX not running", toolkitNotRunning);
        }
    }

    // Translates the report and shows the right kind of dialog. Always runs on the JavaFX thread.
    private static void present(ErrorReport report) {
        if (presenting) {
            log.warn("Error dialog already showing, new error only logged: {}", report.getMessageKey());
            return;
        }
        presenting = true;
        try {
            LocalizedError error = FORMATTER.format(report);

            if (report.isUnexpected()) {
                AlertHelper.showException(error.getTitle(), error.getMessage(), asException(report.getCause()));
            } else {
                AlertHelper.showError(error.getTitle(), error.getMessage());
            }
        } catch (RuntimeException presentationError) {
            log.error("Could not show error alert", presentationError);
        } finally {
            presenting = false;
        }
    }

    // Logs at different levels depending on severity.
    private static void logReport(ErrorReport report) {
        if (report.isUnexpected()) {
            log.error("Unexpected error ({})", report.getMessageKey(), report.getCause());
        } else {
            log.info("Business error ({}): {}", report.getMessageKey(),
                    report.getCause() == null ? "null" : report.getCause().getMessage());
        }
    }

    /* AlertHelper.showException takes an Exception, but the global handler
     * can receive an Error. Then it is wrapped so the stack trace can still be shown.  */
    private static Exception asException(Throwable cause) {
        if (cause instanceof Exception) {
            return (Exception) cause;
        }
        return new RuntimeException(cause);
    }
}
