package com.wac.autocore.exception.handler;

/**
 * <b>ErrorHandlerChain</b>
 * <p>Ansvar: Bygger applikationens standardkedja för felhantering på ett enda ställe.
 * Ordningen är ett designbeslut: de mest specifika leden ligger först och reservledet sist.</p>
 * <p>Ett nytt led läggs till genom att skapa en ny subklass till {@link ErrorHandler} och koppla in
 * den här, före reservledet. Inga befintliga led behöver ändras.</p>
 * <p><b>Exempel på användning:</b></p>
 * <pre>{@code
 * ErrorHandler chain = ErrorHandlerChain.createDefault();
 * ErrorReport report = chain.resolve(exception);
 * }</pre>
 */
public final class ErrorHandlerChain {

    private ErrorHandlerChain() {
    }

    public static ErrorHandler createDefault() {
        ErrorHandler head = new DomainErrorHandler();
        head.linkWith(new DomainErrorHandler())
                .linkWith(new FallbackErrorHandler());
        return head;
    }
}
