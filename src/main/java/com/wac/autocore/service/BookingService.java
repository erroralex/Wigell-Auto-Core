package com.wac.autocore.service;

import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.BookingServiceItem;
import com.wac.autocore.model.BookingStatus;
import com.wac.autocore.model.Mechanic;
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

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// Samordnar bokningar. Servicen laddar data, anropar domänen och kontrollerar
// regler som kräver andra bokningar (fordonet bokat samma dag; avbokade bokningar räknas inte).
// Regler som bara rör en bokning ligger i {@link Booking}.
// Mekanikern på en bokning är ett valfritt förslag och krockkontrolleras inte här;
// mekanikern reserveras först på arbetsordern (se WorkOrderService).
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

    // Alla bokningar utom avbokade.
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

    // Skapar en ny bokning med data från en tidigare bokning.
    // Om mekaniker, fordon, beskrivning eller tjänster inte anges ärvs de från den tidigare bokningen.
    @Transactional
    public BookingCopyResult createFromPrevious(int previousBookingId,
                                                LocalDate date,
                                                LocalTime startTime,
                                                @Positive Integer mechanicId,
                                                @Positive Integer vehicleId,
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
    public Booking update(int bookingId, int vehicleId, @Positive Integer mechanicId,
                          LocalDate date,
                          LocalTime startTime,
                          @NotBlank(message = "error.desc") String description,
                          List<@NotNull @Positive Integer> serviceItemIds) {
        Booking booking = requireBooking(bookingId);
        Vehicle vehicle = requireVehicle(vehicleId);
        requireSuggestedMechanic(mechanicId);

        booking.update(vehicleId, mechanicId, date, startTime, description,
                serviceItemRepository.requireAllById(serviceItemIds));

        // Om kontrollen nedan kastar rullar transaktionen tillbaka ändringen ovan.
        checkVehicleAvailable(booking, vehicle);
        return bookingRepository.save(booking);
    }

    // Bokningen följer sin arbetsorder: datum, starttid och tjänster. Anropas av WorkOrderService i samma transaktion,
    // så ett fel här (fordonet upptaget, midnatt) rullar tillbaka även orderns ändring.
    @Transactional
    public Booking followWorkOrder(int bookingId, LocalDate date, LocalTime startTime,
                                   List<@NotNull @Positive Integer> serviceItemIds) {
        Booking booking = requireBooking(bookingId);
        Vehicle vehicle = requireVehicle(booking.getVehicleId());

        booking.applyWorkOrderPlan(date, startTime, serviceItemRepository.requireAllById(serviceItemIds));

        checkVehicleAvailable(booking, vehicle);
        return bookingRepository.save(booking);
    }

    // Bokningens status följer sin arbetsorder. Anropas av WorkOrderService i samma transaktion.
    // En saknad bokning ger samma fel som när planen följer ordern, i stället för att tyst ignoreras.
    @Transactional
    public Booking followWorkOrderStatus(int bookingId, WorkOrderStatus workOrderStatus) {
        Booking booking = requireBooking(bookingId);
        booking.followWorkOrder(workOrderStatus);
        return bookingRepository.save(booking);
    }

    // Mjuk borttagning: raden och bokningsnumret finns kvar, bara statusen ändras. Domänen avgör om det är tillåtet.
    @Transactional
    public Booking cancel(int bookingId) {
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

    private Booking requireBooking(int id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Booking", id, "error.bookingNotFound"));
    }

    private Vehicle requireVehicle(int id) {
        return vehicleRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Vehicle", id));
    }

    // Förslaget är valfritt, men ett angivet id måste finnas så att ett skräp-id stoppas i appen och inte i databasen.
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

    // Jämför varje rads avtalade pris med dagens katalogpris.
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

    public boolean isVehicleBooked(int vehicleId, LocalDate date) {
        return bookingRepository.existsByVehicleIdAndDateAndStatusNot(vehicleId, date, BookingStatus.CANCELLED);
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
        return serviceItemIdsOf(requireBooking(id));
    }

    @Transactional(readOnly = true)
    public List<BookingPriceChange> findPriceChanges(int id) {
        return priceChanges(requireBooking(id));
    }
}
