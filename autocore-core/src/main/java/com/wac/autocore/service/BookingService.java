package com.wac.autocore.service;

import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.BookingServiceItem;
import com.wac.autocore.model.BookingStatus;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.MechanicRepository;
import com.wac.autocore.repository.ServiceItemRepository;
import com.wac.autocore.repository.VehicleRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// Coordinates bookings. The service loads data, calls the domain and checks
// rules that require other bookings (vehicle booked the same day; cancelled bookings do not count).
// Rules that concern only one booking live in {@link Booking}.
// The mechanic on a booking is an optional suggestion and is not checked for clashes here;
// the mechanic is reserved only on the work order (see WorkOrderService).
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

    // All bookings except cancelled ones.
    public List<Booking> listActive() {
        return bookingRepository.findByStatusNot(BookingStatus.CANCELLED);
    }


    public Optional<Booking> findById(int id) {
        return bookingRepository.findById(id);
    }

    @Transactional
    public Booking create(@Positive @NotNull(message="error.booking.noSelectedVehicle") Integer vehicleId,
                          @Positive (message="error.booking.noSelectedMechanic") Integer mechanicId,
                          LocalDate date,
                          LocalTime startTime,
                          @NotBlank(message = "error.desc") String description,
                          List<@NotNull @Positive Integer> serviceItemIds) {
        Vehicle vehicle = requireVehicle(vehicleId);
        requireSuggestedMechanic(mechanicId);

        Booking booking = new Booking(vehicleId, mechanicId, date, startTime, description,
                serviceItemRepository.requireAllById(serviceItemIds));

        checkVehicleAvailable(booking, vehicle);
        return bookingRepository.save(booking);
    }

    // Creates a new booking with data from a previous booking.
    // If mechanic, vehicle, description or services are not given, they are inherited from the previous booking.
    @Transactional
    public BookingCopyResult createFromPrevious(int previousBookingId,
                                                LocalDate date,
                                                LocalTime startTime,
                                                @Positive Integer mechanicId,
                                                @Positive @NotNull(message="error.vehicleSelect") Integer vehicleId,
                                                String description,
                                                List<@NotNull @Positive Integer> serviceItemIds) {
        Booking previous = requireBooking(previousBookingId);

        int selectedVehicleId = vehicleId != null ? vehicleId : previous.getVehicleId();
        Integer selectedMechanicId = mechanicId != null ? mechanicId : previous.getMechanicId();
        Vehicle vehicle = requireVehicle(selectedVehicleId);
        requireSuggestedMechanic(selectedMechanicId);

        boolean inheritsServices = serviceItemIds == null || serviceItemIds.isEmpty();
        boolean pureCopy = inheritsServices && vehicleId == null && mechanicId == null && description == null;

        Booking copy;
        if (pureCopy) {
            copy = previous.copyAsNew(date, startTime, serviceItemRepository.findAll());
        } else {
            List<Integer> chosenIds = inheritsServices ? serviceItemIdsOf(previous) : serviceItemIds;
            copy = new Booking(selectedVehicleId, selectedMechanicId, date, startTime,
                    description != null ? description : previous.getDescription(),
                    serviceItemRepository.requireAllById(chosenIds));
        }

        checkVehicleAvailable(copy, vehicle);

        Booking savedBooking = bookingRepository.save(copy);

        List<Integer> bookedIds = serviceItemIdsOf(copy);
        List<BookingPriceChange> priceChanges = priceChanges(previous).stream()
                .filter(change -> bookedIds.contains(change.getServiceItemId()))
                .collect(Collectors.toList());

        return new BookingCopyResult(savedBooking, priceChanges);
    }

    @Transactional
    public Booking update(@Positive @NotNull(message="error.noBooking")Integer bookingId,
                          @Positive @NotNull(message="error.vehicleSelect") Integer vehicleId,
                          @Positive Integer mechanicId,
                          LocalDate date,
                          LocalTime startTime,
                          @NotBlank(message = "error.desc") String description,
                          List<@NotNull @Positive Integer> serviceItemIds) {
        Booking booking = requireBooking(bookingId);
        Vehicle vehicle = requireVehicle(vehicleId);
        requireSuggestedMechanic(mechanicId);

        booking.update(vehicleId, mechanicId, date, startTime, description,
                serviceItemRepository.requireAllById(serviceItemIds));

        // If the check below throws, the transaction rolls back the change above.
        checkVehicleAvailable(booking, vehicle);
        return bookingRepository.save(booking);
    }

    // The booking follows its work order: date, start time and services. Called by WorkOrderService in the same transaction,
    // so an error here (vehicle busy, midnight) also rolls back the order's change.
    @Transactional
    public Booking followWorkOrder(@Positive @NotNull(message="error.noBooking") Integer bookingId,
                                   LocalDate date, LocalTime startTime,
                                   List<@NotNull @Positive Integer> serviceItemIds) {
        Booking booking = requireBooking(bookingId);
        Vehicle vehicle = requireVehicle(booking.getVehicleId());

        booking.applyWorkOrderPlan(date, startTime, serviceItemRepository.requireAllById(serviceItemIds));

        checkVehicleAvailable(booking, vehicle);
        return bookingRepository.save(booking);
    }

    // The booking's status follows its work order. Called by WorkOrderService in the same transaction.
    // A missing booking gives the same error as when the plan follows the order, instead of being silently ignored.
    @Transactional
    public Booking followWorkOrderStatus(@Positive @NotNull(message="error.noBooking") Integer bookingId, WorkOrderStatus workOrderStatus) {
        Booking booking = requireBooking(bookingId);
        booking.followWorkOrder(workOrderStatus);
        return bookingRepository.save(booking);
    }

    // Soft delete: the row and the booking number remain, only the status changes. The domain decides whether it is allowed.
    @Transactional
    public Booking cancel(@Positive @NotNull(message="error.noBooking") Integer bookingId) {
        Booking booking = requireBooking(bookingId);
        booking.cancel();
        return bookingRepository.save(booking);
    }

    private void checkVehicleAvailable(Booking booking, Vehicle vehicle) {
        if (bookingRepository.existsByVehicleIdAndDateAndIdNotAndStatusNot(
                booking.getVehicleId(), booking.getDate(), booking.getId(), BookingStatus.CANCELLED)) {
            throw new ValidationException("error.vehicleBooked", vehicle.getRegistrationNumber());
        }
    }

    private Booking requireBooking(@Positive @NotNull(message="error.noBooking") Integer id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Booking", id, "error.bookingNotFound"));
    }

    private Vehicle requireVehicle(Integer id) {
        return vehicleRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Vehicle", id));
    }

    // The suggestion is optional, but a given id must exist so a garbage id is stopped in the app and not in the database.
    private void requireSuggestedMechanic(Integer id) {
        if (id != null && !mechanicRepository.findById(id).isPresent()) {
            throw new EntityNotFoundException("Mechanic", id);
        }
    }

    private static List<Integer> serviceItemIdsOf(Booking booking) {
        return booking.getItems().stream()
                .map(BookingServiceItem::getServiceItemId)
                .collect(Collectors.toList());
    }

    // Compares each line's agreed price with today's catalog price.
    private List<BookingPriceChange> priceChanges(Booking booking) {
        List<BookingPriceChange> changes = new ArrayList<>();
        for (BookingServiceItem item : booking.getItems()) {
            ServiceItem current = serviceItemRepository.requireById(item.getServiceItemId());
            if (item.getPriceAtBooking().compareTo(current.getPrice()) != 0) {
                changes.add(new BookingPriceChange(
                        item.getServiceItemId(),
                        current.getName(),
                        item.getPriceAtBooking(),
                        current.getPrice()
                ));
            }
        }
        return changes;
    }

    public boolean isVehicleBooked(Integer vehicleId, LocalDate date) {
        return bookingRepository.existsByVehicleIdAndDateAndStatusNot(vehicleId, date, BookingStatus.CANCELLED);
    }

    @Transactional(readOnly = true)
    public List<Integer> listServiceItemIds(Integer id) {
        return serviceItemIdsOf(requireBooking(id));
    }

    @Transactional(readOnly = true)
    public List<BookingPriceChange> findPriceChanges(int id) {
        return priceChanges(requireBooking(id));
    }
}
