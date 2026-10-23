package com.wac.autocore.view.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <b>GlobalExceptionHandler</b>
 * <p>Ansvar: Fångar alla undantag som ingen annan kod har fångat och skickar dem vidare till {@link ErrorFacade}.
 * Utan den skrivs sådana fel bara ut i konsolen och användaren ser ingenting.</p>
 * <p>Fångade och ofångade fel går därmed samma väg: felkedja, loggning och feldialog.</p>
 * <p>Om själva felhanteringen misslyckas loggas både det ursprungliga felet och hanteringsfelet via SLF4J.
 * Bara om loggningen också misslyckas skrivs en sista rad till System.err.</p>
 */
public final class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private GlobalExceptionHandler() {
    }

    // Registrerar hanteraren som standard för alla trådar, måste anropas från JavaFX-tråden, t.ex först i start()
    public static void install() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(handler);
        Thread.currentThread().setUncaughtExceptionHandler(handler);
    }

    // Anropas av JVM:en när ett undantag når toppen av en tråd utan att ha fångats.
    @Override
    public void uncaughtException(Thread thread, Throwable error) {
        try {
            ErrorFacade.handle(error);
        } catch (Throwable handlingFailed) {
            logHandlingFailure(thread, error, handlingFailed);
        }
    }

    // Loggar originalfelet som suppressed på hanteringsfelet, så att båda hamnar i samma loggpost.
    private static void logHandlingFailure(Thread thread, Throwable error, Throwable handlingFailed) {
        try {
            if (handlingFailed != error) {
                handlingFailed.addSuppressed(error);
            }
            log.error("Error handling failed for uncaught exception in thread {}", thread.getName(), handlingFailed);
        } catch (Throwable loggingFailed) {
            // System.err som sista utväg
            System.err.println("Logging failed for uncaught exception in thread " + thread.getName() + ": " + error);
        }
    }
}
