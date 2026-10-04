package com.wac.autocore.exception;

import java.util.Objects;

/**
 * <b>DomainException</b>
 * <p>Ansvar: Basklass för alla förväntade affärsfel i applikationen, t.ex. dubbelbokning,
 * låst bokning eller valideringsfel.</p>
 * <p>Undantaget bär språknycklar i stället för färdig text. Tjänstelagret avgör <i>vad</i> som
 * gick fel, och UI-lagret avgör <i>hur</i> och på vilket språk det visas. Därför behöver
 * tjänsterna inte känna till {@code LanguageManager} eller JavaFX, och språkbyte fungerar
 * eftersom texten slås upp först när den visas.</p>
 * <p>{@link #getMessage()} innehåller ett tekniskt meddelande på engelska för loggar,
 * inte för användaren.</p>
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

    /** Ansvar: Språknyckel för feldialogens rubrik, t.ex. {@code error.booking}. */
    public String getTitleKey() {
        return titleKey;
    }

    /** Ansvar: Språknyckel för meddelandet till användaren, t.ex. {@code error.mechanicBusy}. */
    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getMessageArgs() {
        return messageArgs.clone();
    }
}