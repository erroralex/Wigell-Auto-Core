package com.wac.autocore.exception;

/**
 * <b>ValidationException</b>
 * <p>Ansvar: Kastas när indata bryter mot en affärsregel. Rubriken är alltid
 * {@code error.validation}; meddelandet väljs med en befintlig språknyckel.</p>
 * <pre>{@code
 * throw new ValidationException("error.requiredField", lang.get("table.regNumber"));
 * }</pre>
 */
public class ValidationException extends DomainException {

    public ValidationException(String messageKey, Object... messageArgs) {
        super("Validation failed: " + messageKey, "error.validation", messageKey, messageArgs);
    }
}
