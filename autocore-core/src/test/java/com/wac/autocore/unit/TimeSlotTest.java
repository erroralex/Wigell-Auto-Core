package com.wac.autocore.unit;

import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.value.TimeSlot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <b>TimeSlotTest</b>
 * <p>Ansvar: Bevisar tidsluckans regler, som bokning och arbetsorder delar: sluttid, att luckan måste sluta
 * före midnatt (gränsfallen testas här och inte i varje aggregat), att angränsande luckor inte överlappar
 * och att intervallet är halvöppet.</p>
 */
class TimeSlotTest {

    private static TimeSlot slot(int hour, int minute, int durationMinutes) {
        return TimeSlot.of(LocalTime.of(hour, minute), durationMinutes);
    }

    @Test
    @DisplayName("Sluttiden är starttid plus längd (08:00 + 165 min = 10:45)")
    void endIsStartPlusDuration() {
        TimeSlot slot = slot(8, 0, 165);

        assertEquals(LocalTime.of(8, 0), slot.getStart());
        assertEquals(LocalTime.of(10, 45), slot.getEnd());
    }

    @Test
    @DisplayName("En lucka som slutar före midnatt godkänns, men 00:00 eller senare avvisas med error.booking.pastMidnight")
    void mustEndBeforeMidnight() {
        TimeSlot beforeMidnight = slot(21, 45, 120);
        assertTrue(beforeMidnight.endsWithinDay());
        assertSame(beforeMidnight, beforeMidnight.requireWithinDay());

        for (TimeSlot tooLate : new TimeSlot[]{slot(22, 0, 120), slot(23, 0, 90)}) {
            assertFalse(tooLate.endsWithinDay(), "Slut " + tooLate.getEnd());
            ValidationException e = assertThrows(ValidationException.class, tooLate::requireWithinDay);
            assertEquals("error.booking.pastMidnight", e.getMessageKey());
        }
    }

    @Test
    @DisplayName("Överlapp: delvis och omslutande överlappar, angränsande gör det inte och sena luckor slår inte runt till morgonen")
    void overlaps() {
        TimeSlot tenToEleven = slot(10, 0, 60);

        assertTrue(tenToEleven.overlaps(slot(10, 30, 60)));
        assertTrue(slot(10, 30, 60).overlaps(tenToEleven));
        assertTrue(tenToEleven.overlaps(slot(10, 15, 15)));
        assertFalse(tenToEleven.overlaps(slot(11, 0, 60)));
        assertFalse(slot(11, 0, 60).overlaps(tenToEleven));
        assertFalse(slot(23, 0, 90).overlaps(slot(0, 0, 60)));
    }

    @Test
    @DisplayName("contains: starttiden ingår, sluttiden inte")
    void containsIsHalfOpen() {
        TimeSlot tenToEleven = slot(10, 0, 60);

        assertTrue(tenToEleven.contains(LocalTime.of(10, 0)));
        assertTrue(tenToEleven.contains(LocalTime.of(10, 59)));
        assertFalse(tenToEleven.contains(LocalTime.of(11, 0)));
        assertFalse(tenToEleven.contains(LocalTime.of(9, 59)));
    }

    @Test
    @DisplayName("Starttid krävs")
    void startIsRequired() {
        assertThrows(NullPointerException.class, () -> TimeSlot.of(null, 60));
    }
}
