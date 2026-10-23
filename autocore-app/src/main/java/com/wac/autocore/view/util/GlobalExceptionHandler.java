package com.wac.autocore.view.util;

/**
 * <b>GlobalExceptionHandler</b>
 * <p>Responsibility: Catches all exceptions that no other code has caught and passes them on to {@link ErrorFacade}.
 * Without it, such errors are only printed to the console and the user sees nothing.</p>
 * <p>Caught and uncaught errors thereby take the same path: error chain, logging and error dialog.</p>
 */
public final class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {

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
            System.err.println("Caught an unhandled exception in thread: " + thread.getName());
            error.printStackTrace();
            handlingFailed.printStackTrace();
        }
    }
}
