package com.wac.autocore.service;

import com.wac.autocore.model.Booking;
import com.wac.autocore.repository.BookingRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;


    public BookingServiceImpl(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }
    @Override
    public List<Booking> listAll() {
        return bookingRepository.findAll();
    }

    @Override
    public Booking findById(int id) {
        return bookingRepository.findById(id);
    }

    @Override
    public List<Booking> findByMechanic(int mechanicId) {
        return bookingRepository.findByMechanicId(mechanicId);
    }

    @Override
    public Booking create(int vehicleId, int mechanicId, LocalDate date, LocalTime startTime, LocalTime endTime, String description) {
        Booking booking = new Booking(
                vehicleId, mechanicId, date, startTime, endTime, description);
        return bookingRepository.save(booking);
    }

    @Override
    public boolean isVehicleBooked(int vehicleId, LocalDate date) {
        return bookingRepository.existsByVehicleIdAndDate(vehicleId, date);
    }
}
