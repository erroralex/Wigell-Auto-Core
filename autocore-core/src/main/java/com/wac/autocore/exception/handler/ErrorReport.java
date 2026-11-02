package com.wac.autocore.exception.handler;

/**
 * <b>ErrorReport</b>
 * <p>Responsibility: The result of the error chain: what should be shown to the user, expressed as language keys.
 * Contains no JavaFX code, which means the chain can be unit tested without a graphical interface.</p>
 * <p>The class is immutable: all fields are {@code final} and {@code messageArgs} is copied in and out.</p>
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
