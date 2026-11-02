package com.wac.autocore.exception.handler;

import java.util.Objects;

/**
 * <b>ErrorHandler</b>
 * <p>Responsibility: A link in the error chain (<b>Chain of Responsibility</b>). Each link decides whether it can handle
 * an error; otherwise the error is passed on to the next link.</p>
 * <p>{@link #resolve(Throwable)} is a <b>Template Method</b>: the traversal of the chain itself is fixed
 * and {@code final}, while the subclasses only fill in the steps {@link #canHandle(Throwable)} and
 * {@link #createReport(Throwable)}.</p>
 * <p><b>Example of how a chain is built:</b></p>
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

    // Attaches the next link and returns it, so the chain can be built fluently
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
