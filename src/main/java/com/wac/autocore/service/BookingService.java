package com.wac.autocore.service;

import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.exception.MechanicDoubleBookingException;
import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.*;
import com.wac.autocore.model.value.Money;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.MechanicRepository;
import com.wac.autocore.repository.ServiceItemRepository;
import com.wac.autocore.repository.VehicleRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Validated
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
    public Booking create(@Positive int vehicleId,
                          @Positive int mechanicId,
                          @NotNull(message = "error.dateSelect") LocalDate date,
                          @NotNull(message = "error.startTimeSelect") LocalTime startTime,
                          @NotBlank(message="error.desc") String description,
                          @NotEmpty(message= "error.serviceSelect") List<@NotNull @Positive Integer> serviceItemIds) {

        validateVehicleAndMechanic(vehicleId, mechanicId);

        List<ServiceItem> serviceItems = fetchValidServiceItemsByIds(serviceItemIds);

        int totalDuration = serviceItems.stream()
                .mapToInt(ServiceItem::getEstimatedMinutes)
                .sum();
        LocalTime endTime = startTime.plusMinutes(totalDuration);

        checkVehicleAvailable(vehicleId, date, null); //null bookingId eftersom bokningen inte är skapad.
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
                                                @NotNull(message = "error.dateSelect") LocalDate date,
                                                @NotNull(message = "error.startTimeSelect") LocalTime startTime,
                                                @Positive Integer mechanicId,
                                                @Positive Integer vehicleId,
                                                String description,
                                                List<@NotNull @Positive Integer> serviceItemIds) {
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

        if (vehicleId != null) copy.setVehicleId(vehicleId);
        if (description != null) copy.setDescription(description);

        if (copy.getDescription() == null || copy.getDescription().trim().isEmpty()) {
            throw new ValidationException("error.desc");
        }

        List<Integer> wantedIds = (serviceItemIds == null || serviceItemIds.isEmpty())
                ? previous.getItems().stream()
                .map(BookingServiceItem::getServiceItemId)
                .collect(Collectors.toList()) : serviceItemIds.stream().distinct().collect(Collectors.toList());

        List<ServiceItem> serviceItems = fetchValidServiceItemsByIds(wantedIds);

        Map<Integer, Money> oldPrices = previous.getItems().stream()
                .collect(Collectors.toMap(
                        BookingServiceItem::getServiceItemId,
                        BookingServiceItem::getPriceAtBooking,
                        (a, b) -> a));

        List<BookingPriceChange> priceChanges = new ArrayList<>();
        copy.getItems().clear();

        for (ServiceItem serviceItem : serviceItems) {
            Money oldPrice = oldPrices.get(serviceItem.getId());
            if (oldPrice != null && oldPrice.compareTo(serviceItem.getPrice()) != 0) {
                priceChanges.add(new BookingPriceChange(
                        serviceItem.getId(),
                        serviceItem.getName(),
                        oldPrice,
                        serviceItem.getPrice()));
            }

            copy.addServiceItem(serviceItem);
        }

        validateVehicleAndMechanic(copy.getVehicleId(), selectedMechanicId);

        copy.setEndTime(startTime.plusMinutes(copy.getTotalDurationMinutes()));

        checkVehicleAvailable(copy.getVehicleId(), date, null);
        checkMechanicOverlap(
                selectedMechanicId, date, startTime, copy.getEndTime(), null
        );

        Booking savedBooking = bookingRepository.save(copy);
        return new BookingCopyResult(savedBooking, priceChanges);
    }

    @Transactional
    public Booking update(int bookingId, int vehicleId, int mechanicId,
                          @NotNull(message = "error.dateSelect") LocalDate date,
                          @NotNull(message = "error.startTimeSelect") LocalTime startTime,
                          @NotBlank(message="error.desc") String description,
                          @NotEmpty(message = "error.serviceSelect") List<@NotNull @Positive Integer> serviceItemIds) {

        Booking booking = findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking", bookingId, "error.bookingNotFound"));

        if (!BookingStatus.BOOKED.equals(booking.getStatus())) {
            throw new ValidationException("error.bookingStarted");
        }

        validateVehicleAndMechanic(vehicleId, mechanicId);

        List<ServiceItem> serviceItems = fetchValidServiceItemsByIds(serviceItemIds);

        int totalDuration = serviceItems.stream()
                .mapToInt(ServiceItem::getEstimatedMinutes)
                .sum();
        LocalTime endTime = startTime.plusMinutes(totalDuration);

        checkVehicleAvailable(vehicleId, date, bookingId);

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

    /**
     * Checks whether the vehicle isn't already booked on the given date.
     * <p>
     * If excludedBookingId is null (creating a new booking), any booking for the
     * vehicle on that date counts as a conflict.
     * If it is set (updating an existing booking), that booking is ignored, so only
     * other bookings for the vehicle on that date count as conflicts.
     *
     * @param vehicleId         the vehicle to check
     * @param date              the date to check
     * @param excludedBookingId the ID of the booking to ignore, or null if there is none
     * @throws ValidationException with {@code error.vehicleBooked} and the vehicle's
     *                             registration number if the vehicle is already booked
     */
    private void checkVehicleAvailable(int vehicleId, LocalDate date, Integer excludedBookingId) {
        boolean booked = excludedBookingId == null
                ? bookingRepository.existsByVehicleIdAndDate(vehicleId, date)
                : bookingRepository.existsByVehicleIdAndDateAndIdNot(vehicleId, date, excludedBookingId);

        if (booked) {
            String registration = vehicleRepo.findById(vehicleId)
                    .map(Vehicle::getRegistrationNumber)
                    .orElse("Unknown");
            throw new ValidationException("error.vehicleBooked", registration);
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

    @Transactional(readOnly = true)
    public List<Integer> listServiceItemIds(int id) {
        Booking booking = findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Booking", id, "error.bookingNotFound"));

        return booking.getItems().stream()
                .map(BookingServiceItem::getServiceItemId)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookingPriceChange> findPriceChanges(int id) {
        Booking previous = findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Booking", id, "error.bookingNotFound"
                ));

        List<BookingPriceChange> changes = new ArrayList<>();

        for (BookingServiceItem item : previous.getItems()) {
            int serviceId = item.getServiceItemId();

            ServiceItem current = findServiceItemById(serviceId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "ServiceItem", serviceId, "error.serviceNotFound"
                    ));

            if (item.getPriceAtBooking().compareTo(current.getPrice()) != 0) {
                changes.add(new BookingPriceChange(
                        serviceId,
                        current.getName(),
                        item.getPriceAtBooking(),
                        current.getPrice()
                ));
            }

        }
        return changes;
    }

    private List<ServiceItem> fetchValidServiceItemsByIds(List<Integer> serviceItemIds) {
        if (serviceItemIds == null || serviceItemIds.isEmpty()) {
            throw new ValidationException("error.serviceSelect");
        }

        List<ServiceItem> serviceItems = new ArrayList<>();
        for (Integer id : new LinkedHashSet<>(serviceItemIds)) {
            if (id == null || id <= 0) {
                throw new EntityNotFoundException("ServiceItem", id, "error.serviceNotFound");
            }

            ServiceItem serviceItem = findServiceItemById(id)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "ServiceItem", id, "error.serviceNotFound"));
            serviceItems.add(serviceItem);
        }

        if (serviceItems.stream()
                .anyMatch(serviceItem -> serviceItem.getEstimatedMinutes() <= 0)) {
            throw new ValidationException("error.serviceItem.invalidDuration");
        }

        return serviceItems;
    }

    private void validateVehicleAndMechanic(int vehicleId, int mechanicId) {
        if (vehicleId <= 0 || !vehicleRepo.existsById(vehicleId)) {
            throw new EntityNotFoundException("Vehicle", vehicleId);
        }
        if (mechanicId <= 0 || !mechanicRepository.existsById(mechanicId)) {
            throw new EntityNotFoundException("Mechanic", mechanicId);
        }
    }

}
