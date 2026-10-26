package com.wac.autocore.exception;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * <b>MechanicDoubleBookingException</b>
 * <p>Thrown when a mechanic is already booked during the selected time. It is a DomainException so the global error handling shows the correct translated text.</p>
 * <p>The technical message identifies the mechanic by id only, since getMessage() ends up in the log.</p>
 */
public class MechanicDoubleBookingException extends DomainException {

    private final int mechanicId;
    private final LocalDate date;
    private final LocalTime startTime;
    private final LocalTime endTime;

    public MechanicDoubleBookingException(int mechanicId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        super("Mechanic double booking: mechanic " + mechanicId + " on " + date + " " + startTime + "-" + endTime,
                "error.booking", "error.mechanicBusy");
        this.mechanicId = mechanicId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public int getMechanicId() {
        return mechanicId;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }
}
