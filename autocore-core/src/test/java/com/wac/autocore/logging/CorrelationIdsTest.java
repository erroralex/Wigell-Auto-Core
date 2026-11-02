package com.wac.autocore.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <b>CorrelationIdsTest</b>
 * <p>Responsibility: Tests the registry directly, without the aspect: the first tag wins, ids are found
 * through a cause chain, null input is ignored and a cause cycle does not hang.</p>
 */
class CorrelationIdsTest {

    @Test
    @DisplayName("The first tag wins")
    void firstTagWins() {
        RuntimeException e = new RuntimeException();
        CorrelationIds.tag(e, "first");
        CorrelationIds.tag(e, "second");

        assertEquals("first", CorrelationIds.find(e));
    }

    @Test
    @DisplayName("The id is found through the cause chain")
    void findsThroughCauseChain() {
        RuntimeException inner = new RuntimeException();
        CorrelationIds.tag(inner, "abc12345");
        RuntimeException outer = new RuntimeException(new IllegalStateException(inner));

        assertEquals("abc12345", CorrelationIds.find(outer));
    }

    @Test
    @DisplayName("Null input is ignored")
    void nullInputs() {
        assertDoesNotThrow(() -> CorrelationIds.tag(null, "x"));

        RuntimeException e = new RuntimeException();
        CorrelationIds.tag(e, null);

        assertNull(CorrelationIds.find(e));
        assertNull(CorrelationIds.find(null));
    }

    @Test
    @DisplayName("A cause cycle does not hang")
    void causeCycleTerminates() {
        RuntimeException a = new RuntimeException();
        RuntimeException b = new RuntimeException();
        a.initCause(b);
        b.initCause(a);

        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> assertNull(CorrelationIds.find(a)));
    }

    @Test
    @DisplayName("New ids are 8 characters and unique")
    void newIdFormat() {
        String first = CorrelationIds.newId();
        String second = CorrelationIds.newId();

        assertEquals(8, first.length());
        assertNotEquals(first, second);
    }
}
