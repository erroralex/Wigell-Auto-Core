package com.wac.autocore.exception.handler;

import java.util.Objects;

/**
 * <b>ErrorHandler</b>
 * <p>Ansvar: Ett led i felkedjan (<b>Chain of Responsibility</b>). Varje led avgör om det kan hantera
 * ett fel; annars skickas felet vidare till nästa led.</p>
 * <p>{@link #resolve(Throwable)} är en <b>Template Method</b>: själva genomgången av kedjan är fast
 * och {@code final}, medan subklasserna bara fyller i stegen {@link #canHandle(Throwable)} och
 * {@link #createReport(Throwable)}.</p>
 * <p><b>Exempel på hur en kedja byggs:</b></p>
 * <pre>{@code
 * ErrorHandler head = new DomainErrorHandler();
 * head.linkWith(new DataAccessErrorHandler())
 *     .linkWith(new FallbackErrorHandler());
 * ErrorReport report = head.resolve(exception);
 * }</pre>
 */
public abstract class ErrorHandler {

    private static final int MAX_CAUSE_DEPTH = 20;
    private ErrorHandler next;

    // Kopplar på nästa led och returnerar det, så att kedjan kan byggas flytande
    public ErrorHandler linkWith(ErrorHandler next) {
        this.next = Objects.requireNonNull(next, "next");
        return next;
    }

    // Template
    public final ErrorReport resolve(Throwable error) {
        if (canHandle(error)) {
            return createReport(error);
        }
        if (next != null) {
            return next.resolve(error);
        }
        return ErrorReport.unexpected(error);
    }

    protected abstract boolean canHandle(Throwable error);

    protected abstract ErrorReport createReport(Throwable error);

    protected static <T extends Throwable> T findCause(Throwable error, Class<T> type) {
        Throwable current = error;
        int depth = 0;
        while (current != null && depth < MAX_CAUSE_DEPTH) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause();
            depth++;
        }
        return null;
    }
}
