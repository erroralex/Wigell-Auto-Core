package com.wac.autocore.exception;

/**
 * <b>ValidationException</b>
 * <p>Responsibility: Thrown when input violates a business rule. The title is always
 * {@code error.validation}; the message is chosen with an existing language key.</p>
 * <pre>{@code
 * throw new ValidationException("error.requiredField", lang.get("table.regNumber"));
 * }</pre>
 */
public class ValidationException extends DomainException {

    public ValidationException(String messageKey, Object... messageArgs) {
        super("Validation failed: " + messageKey, "error.validation", messageKey, messageArgs);
    }
}
