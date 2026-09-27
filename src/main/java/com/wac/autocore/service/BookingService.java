package com.wac.autocore.service;

import com.wac.autocore.exception.MechanicDoubleBookingException;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.MechanicRepository;
import com.wac.autocore.repository.ServiceItemRepository;
import com.wac.autocore.repository.VehicleRepo;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final MechanicRepository mechanicRepository;
    private final VehicleRepo vehicleRepo;

    public BookingService(BookingRepository bookingRepository,
                          ServiceItemRepository serviceItemRepository,
                          MechanicRepository mechanicRepository,
                          VehicleRepo vehicleRepo) {
        this.bookingRepository = bookingRepository;
        this.serviceItemRepository = serviceItemRepository;
        this.mechanicRepository = mechanicRepository;
        this.vehicleRepo = vehicleRepo;
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


    public Booking create(int vehicleId, int mechanicId, LocalDate date, LocalTime startTime, LocalTime endTime, String description, int serviceItemId) {

        ServiceItem serviceItem = findServiceItemById(serviceItemId)
                .orElseThrow(() -> new RuntimeException("Service item not found: " + serviceItemId));

        List<Booking> mechanicBookingsSameDay = bookingRepository.findBookingByMechanicIdAndDate(mechanicId, date);

        boolean overlaps = mechanicBookingsSameDay.stream()
                .anyMatch(b -> b.getStartTime().isBefore(endTime) && b.getEndTime().isAfter(startTime));

        if (overlaps) {
            String mechanicName = mechanicRepository.findById(mechanicId)
                    .map(Mechanic::getName)
                    .orElse("Unknown");
            throw new MechanicDoubleBookingException(
                    mechanicName, date, startTime, endTime
            );
        }

        Booking booking = new Booking(
                vehicleId, mechanicId, date, startTime, endTime, description);

        Set<ServiceItem> serviceItems = new HashSet<>();
        serviceItems.add(serviceItem);
        booking.setServiceItems(serviceItems);

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

    public Optional<ServiceItem> findServiceItemById(int id) {
        return serviceItemRepository.findById(id);
    }

    public List<Mechanic> listAllMechanics() {
        return mechanicRepository.findAll();
    }

    public List<Vehicle> listAllVehicles() {
        return vehicleRepo.findAll();
    }


}
