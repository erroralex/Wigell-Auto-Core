package com.wac.autocore.logging;

import com.wac.autocore.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>SafeLogTest</b>
 * <p>Responsibility: Proves that only our own domain messages reach the log, and that messages from
 * other exceptions, which can echo user input, are reduced to their type.</p>
 */
class SafeLogTest {

    @Test
    @DisplayName("Domain exception: type and technical message are kept")
    void domainExceptionKeepsMessage() {
        String text = SafeLog.describe(new ValidationException("error.fields"));

        assertTrue(text.startsWith("ValidationException: "), text);
        assertTrue(text.contains("error.fields"), text);
    }

    @Test
    @DisplayName("Other exception: only the type, never the message")
    void otherExceptionHidesMessage() {
        String userInput = "Testa Testsson 070-1234567";
        String text = SafeLog.describe(new IllegalArgumentException("For input string: " + userInput));

        assertEquals("IllegalArgumentException", text);
    }

    @Test
    @DisplayName("Null gives a placeholder instead of an exception")
    void nullGivesPlaceholder() {
        assertEquals("none", SafeLog.describe(null));
    }
}
