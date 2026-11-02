package com.wac.autocore.logging;

import com.wac.autocore.exception.DomainException;

/**
 * <b>SafeLog</b>
 * <p>Responsibility: Describes an exception for a log line without leaking personal data or
 * user-entered text.</p>
 * <p>Only {@link DomainException} messages are included, because they are written by us and must
 * contain ids and keys only. Messages from other exceptions (validation, database, JDK) are not under
 * our control and can echo input, so only the exception type is logged for them.</p>
 */
public final class SafeLog {

    private SafeLog() {
    }

    /* Returns a log-safe description of the error.
     * The error to describe, may be null
     * Return the type and, for domain exceptions, the technical message; "none" for null */
    public static String describe(Throwable error) {
        if (error == null) {
            return "none";
        }
        String type = error.getClass().getSimpleName();
        if (error instanceof DomainException) {
            return type + ": " + error.getMessage();
        }
        return type;
    }
}
