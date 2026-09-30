package com.wac.autocore.exception.handler;

/**
 * <b>ErrorReport</b>
 * <p>Ansvar: Resultatet av felkedjan: Vad som ska visas för användaren, uttryckt som språknycklar.
 * Innehåller ingen JavaFX-kod, vilket gör att kedjan kan enhetstestas utan grafiskt gränssnitt.</p>
 * <p>Klassen är oföränderlig: alla fält är {@code final} och {@code messageArgs} kopieras in och ut.</p>
 */
public final class ErrorReport {

    public enum Severity {
        EXPECTED,
        UNEXPECTED
    }

    private final Severity severity;
    private final String titleKey;
    private final String messageKey;
    private final Object[] messageArgs;
    private final Throwable cause;

    public ErrorReport(Severity severity, String titleKey, String messageKey, Object[] messageArgs, Throwable cause) {
        this.severity = severity;
        this.titleKey = titleKey;
        this.messageKey = messageKey;
        this.messageArgs = messageArgs == null ? new Object[0] : messageArgs.clone();
        this.cause = cause;
    }

    public static ErrorReport unexpected(Throwable cause) {
        return new ErrorReport(Severity.UNEXPECTED,"error.unexpected", "error.unexpectedMsg", null, cause);
    }

    public Severity getSeverity() {
        return severity;
    }

    public boolean isUnexpected() {
        return severity == Severity.UNEXPECTED;
    }

    public String getTitleKey() {
        return titleKey;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getMessageArgs() {
        return messageArgs.clone();
    }

    public Throwable getCause() {
        return cause;
    }
}
