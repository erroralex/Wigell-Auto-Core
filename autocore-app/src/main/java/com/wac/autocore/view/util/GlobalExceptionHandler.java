package com.wac.autocore.view.util;

/**
 * <b>GlobalExceptionHandler</b>
 * <p>Ansvar: Fångar alla undantag som ingen annan kod har fångat och skickar dem vidare till {@link ErrorFacade}.
 * Utan den skrivs sådana fel bara ut i konsolen och användaren ser ingenting.</p>
 * <p>Fångade och ofångade fel går därmed samma väg: felkedja, loggning och feldialog.</p>
 */
public final class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {

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
            System.err.println("Caught an unhandled exception in thread: " + thread.getName());
            error.printStackTrace();
            handlingFailed.printStackTrace();
        }
    }
}
