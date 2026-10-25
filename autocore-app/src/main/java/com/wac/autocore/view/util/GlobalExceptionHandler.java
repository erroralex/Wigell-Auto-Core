package com.wac.autocore.view.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <b>GlobalExceptionHandler</b>
 * <p>Responsibility: Catches all exceptions that no other code has caught and passes them on to {@link ErrorFacade}.
 * Without it, such errors are only printed to the console and the user sees nothing.</p>
 * <p>Caught and uncaught errors thereby take the same path: error chain, logging and error dialog.</p>
 * <p>If the error handling itself fails, both the original error and the handling error are logged via SLF4J.
 * Only if logging also fails is a final line written to System.err.</p>
 */
public final class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private GlobalExceptionHandler() {
    }

    // Registers the handler as the default for all threads, must be called from the JavaFX thread, e.g. first in start()
    public static void install() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(handler);
        Thread.currentThread().setUncaughtExceptionHandler(handler);
    }

    // Called by the JVM when an exception reaches the top of a thread without having been caught.
    @Override
    public void uncaughtException(Thread thread, Throwable error) {
        try {
            ErrorFacade.handle(error);
        } catch (Throwable handlingFailed) {
            logHandlingFailure(thread, error, handlingFailed);
        }
    }
  
    // Logs the original error as suppressed on the handling error, so both end up in the same log entry.
    private static void logHandlingFailure(Thread thread, Throwable error, Throwable handlingFailed) {
        try {
            if (handlingFailed != error) {
                handlingFailed.addSuppressed(error);
            }
            log.error("Error handling failed for uncaught exception in thread {}", thread.getName(), handlingFailed);
        } catch (Throwable loggingFailed) {
            // Last resort: logging itself failed, so System.err is the only channel left.
            System.err.println("Logging failed for uncaught exception in thread " + thread.getName() + ": " + error);
        }
    }
}
