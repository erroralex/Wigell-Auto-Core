package com.wac.autocore.service;

import com.wac.autocore.model.Booking;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface BookingService {

    List<Booking> listAll();
    Booking findById(int id);
    List<Booking> findByMechanic(int mechanicId);

    Booking create(int vehicleId,
                   int mechanicId,
                   LocalDate date,
                   LocalTime startTime,
                   LocalTime endTime,
                   String description);

    boolean isVehicleBooked(int vehicleId, LocalDate date);
}
