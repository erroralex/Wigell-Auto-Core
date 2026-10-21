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
 * <p>Ansvar: En enda ingång (<b>Facade</b>) för att visa fel i gränssnittet. Texten översätts av {@code ErrorReportFormatter},
 * samma som i headless-läget, så att felen ser likadana ut i båda.</p>
 * <p>Fel som inte fångas alls hamnar här automatiskt via {@link GlobalExceptionHandler}.</p>
 * <p><b>Exempel på användning i en vy:</b></p>
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

    // Loggar felet och visar det för användaren.
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

    // Översätter rapporten och visar rätt sorts dialog. Körs alltid på JavaFX-tråden.
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

    // Loggar på olika nivåer beroende på allvar.
    private static void logReport(ErrorReport report) {
        if (report.isUnexpected()) {
            log.error("Unexpected error ({})", report.getMessageKey(), report.getCause());
        } else {
            log.info("Business error ({}): {}", report.getMessageKey(),
                    report.getCause() == null ? "null" : report.getCause().getMessage());
        }
    }

    /* AlertHelper.showException tar ett Exception, men den globala hanteraren
     * kan få ett Error. Då lindas det in så att stack tracen ändå kan visas.  */
    private static Exception asException(Throwable cause) {
        if (cause instanceof Exception) {
            return (Exception) cause;
        }
        return new RuntimeException(cause);
    }
}
