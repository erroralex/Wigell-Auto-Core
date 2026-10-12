package com.wac.autocore.model;

import java.time.LocalDate;
import java.time.LocalTime;

// Används av BookingService.copyBookingAsNew() för att skapa en ny bokning baserat på en befintlig bokning.
public interface BookingPrototype {

    Booking copyAsNew(LocalDate date, LocalTime startTime);

}
