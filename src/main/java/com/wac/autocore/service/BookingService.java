package com.wac.autocore.service;

import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.exception.MechanicDoubleBookingException;
import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.BookingPrototype;
import com.wac.autocore.model.BookingServiceItem;
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
import java.util.ArrayList;
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

        // Använder den nya metoden för att kontrollera överlappning av mekanikerbokningar
        checkMechanicOverlap(mechanicId, date, startTime, endTime, null);

        Booking booking = new Booking(
                vehicleId, mechanicId, date, startTime, endTime, description
        );

        serviceItems.forEach(booking::addServiceItem);

        return bookingRepository.save(booking);
    }

    // Skapar en ny bokning från en tidigare med dagens tjänstepriser, utan att ändra originalet.
    // Återanvänder originalets mekaniker om mechanicId är null och kontrollerar dubbelbokning före sparandet.
    @Transactional
    public BookingCopyResult createFromPrevious(int previousBookingId,
                                                LocalDate date,
                                                LocalTime startTime,
                                                Integer mechanicId) {
        Booking previous = findById(previousBookingId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Booking", previousBookingId, "error.bookingNotFound"
                ));

        if (previous.getItems().isEmpty()) {
            throw new ValidationException("error.serviceSelect");
        }

        int selectedMechanicId = mechanicId == null
                ? previous.getMechanicId()
                : mechanicId;

        if (!mechanicRepository.existsById(selectedMechanicId)) {
            throw new EntityNotFoundException("Mechanic", selectedMechanicId);
        }

        BookingPrototype prototype = previous;
        Booking copy = prototype.copyAsNew(date, startTime);
        copy.setMechanicId(selectedMechanicId);

        List<BookingPriceChange> priceChanges = new ArrayList<>();

        copy.getItems().clear();

        for (BookingServiceItem previousItem : previous.getItems()) {
            int serviceId = previousItem.getServiceItemId();

            ServiceItem currentService = findServiceItemById(serviceId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "ServiceItem", serviceId, "error.serviceNotFound"
                    ));

            if (Double.compare(previousItem.getPriceAtBooking(),
                    currentService.getPrice()) != 0) {
                priceChanges.add(new BookingPriceChange(
                        serviceId,
                        currentService.getName(),
                        previousItem.getPriceAtBooking(),
                        currentService.getPrice()
                ));
            }

            copy.addServiceItem(currentService);
        }

        copy.setEndTime(startTime.plusMinutes(copy.getTotalDurationMinutes()));

        checkMechanicOverlap(
                selectedMechanicId, date, startTime, copy.getEndTime(), null
        );

        Booking savedBooking = bookingRepository.save(copy);

        return new BookingCopyResult(savedBooking, priceChanges);
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

        // Använder den nya metoden för att kontrollera överlappning av mekanikerbokningar,
        // exkluderar den aktuella bokningen från kontrollen med excludedBookingId
        checkMechanicOverlap(mechanicId, date, startTime, endTime, bookingId);

        booking.setVehicleId(vehicleId);
        booking.setMechanicId(mechanicId);
        booking.setDate(date);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setDescription(description);

        List<Integer> oldServiceItemIds = booking.getItems().stream()
                .map(BookingServiceItem::getServiceItemId)
                .collect(Collectors.toList());


        oldServiceItemIds.stream()
                .filter(id -> !serviceItemIds.contains(id))
                .forEach(booking::removeServiceItem);

        serviceItems.stream()
                .filter(serviceItem -> !oldServiceItemIds.contains(serviceItem.getId()))
                .forEach(booking::addServiceItem);

        return bookingRepository.save(booking);
    }

    // Metod för att kontrollera överlappning av mekanikerbokningar
    private void checkMechanicOverlap(int mechanicId,
                                      LocalDate date,
                                      LocalTime startTime,
                                      LocalTime endTime,
                                      Integer excludedBookingId) {
        List<Booking> mechanicBookingSameDay =
                bookingRepository.findBookingByMechanicIdAndDate(mechanicId, date);

        boolean overlaps = mechanicBookingSameDay.stream()
                .filter(b -> excludedBookingId == null
                        || b.getId() != excludedBookingId)
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
