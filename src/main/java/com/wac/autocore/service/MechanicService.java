package com.wac.autocore.service;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.MechanicRepository;
import com.wac.autocore.repository.VehicleRepo;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class MechanicService {

    private final MechanicRepository mechanicRepository;
    private final BookingRepository bookingRepository;
    private final VehicleRepo vehicleRepo;

    public MechanicService(MechanicRepository mechanicRepository, BookingRepository bookingRepository, VehicleRepo vehicleRepo) {
        this.mechanicRepository = mechanicRepository;
        this.bookingRepository = bookingRepository;
        this.vehicleRepo = vehicleRepo;
    }

    public List<Mechanic> listAll() {
        return mechanicRepository.findAll();
    }

    public Optional<Mechanic> findById(int id) {
        return mechanicRepository.findById(id);
    }

    public boolean isAvailable(int mechanicId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        return bookingRepository.findBookingByMechanicIdAndDate(mechanicId, date).stream()
                .noneMatch(b -> b.getStartTime().isBefore(endTime) && b.getEndTime().isAfter(startTime));
    }

    public boolean isBusyNow(int mechanicId) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        return bookingRepository.findBookingByMechanicIdAndDate(mechanicId, today).stream()
                .anyMatch(b -> !b.getStartTime().isAfter(now) && b.getEndTime().isAfter(now));
    }

    public List<Booking> listBookingsByMechanicId(int mechanicId) {
        return bookingRepository.findByMechanicId(mechanicId);
    }

    public Optional<Vehicle> getVehicleById(int vehicleId) {
        return vehicleRepo.findById(vehicleId);
    }
}
