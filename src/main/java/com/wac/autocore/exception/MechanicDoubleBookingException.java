package com.wac.autocore.exception;

import java.time.LocalDate;
import java.time.LocalTime;

public class MechanicDoubleBookingException extends RuntimeException {

    private final String mechanicName;
    private final LocalDate date;
    private final LocalTime startTime;
    private final LocalTime endTime;

    public MechanicDoubleBookingException(String mechanicName, LocalDate date, LocalTime startTime, LocalTime endTime) {
        super("Mechanic double booking: " + mechanicName + " on " + date + " " + startTime + "-" + endTime);
        this.mechanicName = mechanicName;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getMechanicName() {
        return mechanicName;
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
