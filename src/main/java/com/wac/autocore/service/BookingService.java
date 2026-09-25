package com.wac.autocore.service;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.MechanicRepository;
import com.wac.autocore.repository.ServiceItemRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final MechanicRepository mechanicRepository;

    public BookingService(BookingRepository bookingRepository,
                          ServiceItemRepository serviceItemRepository,
                          MechanicRepository mechanicRepository) {
        this.bookingRepository = bookingRepository;
        this.serviceItemRepository = serviceItemRepository;
        this.mechanicRepository = mechanicRepository;
    }

    public List<Booking> listAll() {
        return bookingRepository.findAll();
    }


    public Optional<Booking> findById(int id) {
        return bookingRepository.findById(id);
    }


    public List<Booking> findByMechanic(int mechanicId) {
        return bookingRepository.findByMechanicId(mechanicId);
    }


    public Booking create(int vehicleId, int mechanicId, LocalDate date, LocalTime startTime, LocalTime endTime, String description) {
        Booking booking = new Booking(
                vehicleId, mechanicId, date, startTime, endTime, description);

        Booking savedBooking = bookingRepository.save(booking);

        mechanicRepository.findById(mechanicId).ifPresent(mechanic -> {
            mechanic.setAvailable(false);
            mechanicRepository.save(mechanic);
        });

        return savedBooking;
    }


    public boolean isVehicleBooked(int vehicleId, LocalDate date) {
        return bookingRepository.existsByVehicleIdAndDate(vehicleId, date);
    }

    public List<ServiceItem> listAllServiceItems() {
        return serviceItemRepository.findAll();
    }

    public List<Mechanic> listAllMechanics() {
        return mechanicRepository.findAll();
    }


}
