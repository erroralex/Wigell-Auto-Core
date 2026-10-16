package com.wac.autocore.model.value;

import com.wac.autocore.exception.ValidationException;

import java.time.LocalTime;
import java.util.Objects;

// Värdeobjekt för en tidslucka under en dag: starttid + längd i minuter. Immutable.
// Räknar i minuter från midnatt, så en lucka som passerar midnatt slår inte runt vid jämförelser.
public final class TimeSlot {

    private static final int MINUTES_PER_DAY = 24 * 60;

    private final LocalTime start;
    private final int durationMinutes;

    private TimeSlot(LocalTime start, int durationMinutes) {
        this.start = Objects.requireNonNull(start, "TimeSlot start must not be null");
        this.durationMinutes = durationMinutes;
    }

    public static TimeSlot of(LocalTime start, int durationMinutes) {
        return new TimeSlot(start, durationMinutes);
    }

    public LocalTime getStart() {
        return start;
    }

    public LocalTime getEnd() {
        return start.plusMinutes(durationMinutes);
    }

    // Samma regel för bokning och arbetsorder: luckan måste sluta före midnatt (00:00 räknas som nästa dag).
    public TimeSlot requireWithinDay() {
        if (!endsWithinDay()) {
            throw new ValidationException("error.booking.pastMidnight");
        }
        return this;
    }

    public boolean endsWithinDay() {
        return endMinute() < MINUTES_PER_DAY;
    }

    // Angränsande luckor (10–11 och 11–12) överlappar inte.
    public boolean overlaps(TimeSlot other) {
        return startMinute() < other.endMinute() && endMinute() > other.startMinute();
    }

    // Starttiden ingår, sluttiden inte (samma halvöppna intervall som overlaps). Räknar i hela minuter.
    public boolean contains(LocalTime time) {
        int minute = time.toSecondOfDay() / 60;
        return startMinute() <= minute && minute < endMinute();
    }

    private int startMinute() {
        return start.toSecondOfDay() / 60;
    }

    private int endMinute() {
        return startMinute() + durationMinutes;
    }
}
