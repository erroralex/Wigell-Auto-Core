package com.wac.autocore.service;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.ServiceItemRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ServiceItemRepository serviceItemRepository;

    public BookingService(BookingRepository bookingRepository, ServiceItemRepository serviceItemRepository) {
        this.bookingRepository = bookingRepository;
        this.serviceItemRepository = serviceItemRepository;
    }

    public List<Booking> listAll() {
        return bookingRepository.findAll();
    }


    public Booking findById(int id) {
        return bookingRepository.findById(id);
    }


    public List<Booking> findByMechanic(int mechanicId) {
        return bookingRepository.findByMechanicId(mechanicId);
    }


    public Booking create(int vehicleId, int mechanicId, LocalDate date, LocalTime startTime, LocalTime endTime, String description) {
        Booking booking = new Booking(
                vehicleId, mechanicId, date, startTime, endTime, description);
        return bookingRepository.save(booking);
    }


    public boolean isVehicleBooked(int vehicleId, LocalDate date) {
        return bookingRepository.existsByVehicleIdAndDate(vehicleId, date);
    }

    public List<ServiceItem> listAllServiceItems() {
        return serviceItemRepository.findAll();
    }
}
