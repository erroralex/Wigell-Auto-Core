package com.wac.autocore.exception.handler;

/**
 * <b>ErrorHandlerChain</b>
 * <p>Responsibility: Builds the application's default error handling chain in a single place.
 * The order is a design decision: the most specific links come first and the fallback link last.</p>
 * <p>A new link is added by creating a new subclass of {@link ErrorHandler} and plugging it in
 * here, before the fallback link. No existing links need to change.</p>
 * <p><b>Example usage:</b></p>
 * <pre>{@code
 * ErrorHandler chain = ErrorHandlerChain.createDefault();
 * ErrorReport report = chain.resolve(exception);
 * }</pre>
 */
public final class ErrorHandlerChain {

    private ErrorHandlerChain() {
    }

    public static ErrorHandler createDefault() {
        ErrorHandler head = new SecurityErrorHandler();
        head.linkWith(new ValidationErrorHandler())
                .linkWith(new DomainErrorHandler())
                .linkWith(new DataAccessErrorHandler())
                .linkWith(new FallbackErrorHandler());
        return head;
    }
}
