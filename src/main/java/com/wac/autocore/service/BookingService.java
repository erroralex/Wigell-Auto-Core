package com.wac.autocore.service;

import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.exception.MechanicDoubleBookingException;
import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.MechanicRepository;
import com.wac.autocore.repository.ServiceItemRepository;
import com.wac.autocore.repository.VehicleRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    @Transactional
    public Booking create(int vehicleId, int mechanicId, LocalDate date,
                          LocalTime startTime, String description,
                          List<Integer> serviceItemIds) {

        if (serviceItemIds == null || serviceItemIds.isEmpty()) {
            throw new ValidationException("error.serviceSelect");
        }

        List<ServiceItem> serviceItems = serviceItemIds.stream()
                .distinct()
                .map(id -> findServiceItemById(id)
                        .orElseThrow(() -> new EntityNotFoundException("ServiceItem", id, "error.serviceNotFound")))
                .collect(Collectors.toList());

        int totalDuration = serviceItems.stream()
                .mapToInt(ServiceItem::getEstimatedMinutes)
                .sum();
        LocalTime endTime = startTime.plusMinutes(totalDuration);

        List<Booking> mechanicBookingsSameDay =
                bookingRepository.findBookingByMechanicIdAndDate(mechanicId, date);

        boolean overlaps = mechanicBookingsSameDay.stream()
                .anyMatch(b -> b.getStartTime().isBefore(endTime)
                        && b.getEndTime().isAfter(startTime));

        if (overlaps) {
            String mechanicName = mechanicRepository.findById(mechanicId)
                    .map(Mechanic::getName)
                    .orElse("Unknown");
            throw new MechanicDoubleBookingException(
                    mechanicName, date, startTime, endTime
            );
        }

        Booking booking = new Booking(
                vehicleId, mechanicId, date, startTime, endTime, description
        );

        serviceItems.forEach(booking::addServiceItem);

        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking update(int bookingId, int vehicleId, int mechanicId, LocalDate date,
                          LocalTime startTime, String description,
                          List<Integer> serviceItemIds) {

        Booking booking = findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking", bookingId, "error.bookingNotFound"));

        if (serviceItemIds == null || serviceItemIds.isEmpty()) {
            throw new ValidationException("error.serviceSelect");
        }

        List<ServiceItem> serviceItems = serviceItemIds.stream()
                .distinct()
                .map(id -> findServiceItemById(id)
                        .orElseThrow(() -> new EntityNotFoundException("ServiceItem", id, "error.serviceNotFound")))
                .collect(Collectors.toList());

        int totalDuration = serviceItems.stream()
                .mapToInt(ServiceItem::getEstimatedMinutes)
                .sum();
        LocalTime endTime = startTime.plusMinutes(totalDuration);

        List<Booking> mechanicBookingsSameDay =
                bookingRepository.findBookingByMechanicIdAndDate(mechanicId, date);

        boolean overlaps = mechanicBookingsSameDay.stream()
                .filter(b -> b.getId() != bookingId)
                .anyMatch(b -> b.getStartTime().isBefore(endTime)
                        && b.getEndTime().isAfter(startTime));

        if (overlaps) {
            String mechanicName = mechanicRepository.findById(mechanicId)
                    .map(Mechanic::getName)
                    .orElse("Unknown");
            throw new MechanicDoubleBookingException(
                    mechanicName, date, startTime, endTime
            );
        }


        booking.setVehicleId(vehicleId);
        booking.setMechanicId(mechanicId);
        booking.setDate(date);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setDescription(description);
        booking.getItems().clear();
        bookingRepository.saveAndFlush(booking);
        serviceItems.forEach(booking::addServiceItem);

        return bookingRepository.save(booking);
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
