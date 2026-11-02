package com.wac.autocore.model.value;

import com.wac.autocore.exception.ValidationException;

import java.time.LocalTime;
import java.util.Objects;

// Value object for a time slot during a day: start time + length in minutes. Immutable.
// Counts in minutes from midnight, so a slot that passes midnight does not wrap around in comparisons.
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

    // Same rule for booking and work order: the slot must end before midnight (00:00 counts as the next day).
    public TimeSlot requireWithinDay() {
        if (!endsWithinDay()) {
            throw new ValidationException("error.booking.pastMidnight");
        }
        return this;
    }

    public boolean endsWithinDay() {
        return endMinute() < MINUTES_PER_DAY;
    }

    // Adjacent slots (10–11 and 11–12) do not overlap.
    public boolean overlaps(TimeSlot other) {
        return startMinute() < other.endMinute() && endMinute() > other.startMinute();
    }

    // The start time is included, the end time is not (the same half-open interval as overlaps). Counts in whole minutes.
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
