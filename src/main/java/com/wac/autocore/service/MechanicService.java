package com.wac.autocore.service;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.MechanicRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class MechanicService {

    private final MechanicRepository mechanicRepository;
    private final BookingRepository bookingRepository;

    public MechanicService(MechanicRepository mechanicRepository, BookingRepository bookingRepository) {
        this.mechanicRepository = mechanicRepository;
        this.bookingRepository = bookingRepository;
    }

    public List<Mechanic> listAll() {
        return mechanicRepository.findAll();
    }

    public Optional<Mechanic> findById(int id) {
        return mechanicRepository.findById(id);
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
}
