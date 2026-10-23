package com.wac.autocore.exception;

import java.util.Objects;

/**
 * <b>DomainException</b>
 * <p>Responsibility: Base class for all expected business errors in the application, e.g. double booking,
 * locked booking or validation errors.</p>
 * <p>The exception carries language keys instead of finished text. The service layer decides <i>what</i>
 * went wrong, and the UI layer decides <i>how</i> and in which language it is shown. That way the
 * services do not need to know about {@code LanguageManager} or JavaFX, and switching language works
 * because the text is looked up only when it is shown.</p>
 * <p>{@link #getMessage()} contains a technical message in English for logs,
 * not for the user.</p>
 */
public abstract class DomainException extends RuntimeException {

    private final String titleKey;
    private final String messageKey;
    private final Object[] messageArgs;

    protected DomainException(String technicalMessage, String titleKey, String messageKey, Object... messageArgs) {
        super(technicalMessage);
        this.titleKey = Objects.requireNonNull(titleKey, "titleKey");
        this.messageKey = Objects.requireNonNull(messageKey, "messageKey");
        this.messageArgs = messageArgs == null ? new Object[0] : messageArgs.clone();
    }

    protected DomainException(String technicalMessage, Throwable cause, String titleKey, String messageKey, Object... messageArgs) {
        super(technicalMessage, cause);
        this.titleKey = Objects.requireNonNull(titleKey, "titleKey");
        this.messageKey = Objects.requireNonNull(messageKey, "messageKey");
        this.messageArgs = messageArgs == null ? new Object[0] : messageArgs.clone();
    }

    /** Responsibility: Language key for the error dialog's title, e.g. {@code error.booking}. */
    public String getTitleKey() {
        return titleKey;
    }

    /** Responsibility: Language key for the message to the user, e.g. {@code error.mechanicBusy}. */
    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getMessageArgs() {
        return messageArgs.clone();
    }
}