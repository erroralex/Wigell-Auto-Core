package com.wac.autocore.service;

import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.BookingStatus;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.model.value.Money;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.MechanicRepository;
import com.wac.autocore.repository.ServiceItemRepository;
import com.wac.autocore.repository.VehicleRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.wac.autocore.testsupport.MoneyAssert.assertMoney;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testar samordningen i {@link BookingService}: att data laddas, att okända id:n avvisas,
 * att fordonet inte är bokat samma dag (avbokade bokningar räknas inte), att avbokning sparas
 * och att inget sparas vid fel. Den föreslagna mekanikern
 * slås bara upp när den är ifylld och krockkontrolleras inte (den reserveras först på arbetsordern).
 * Bokningens egna regler testas i {@code BookingTest}; att avbokade bokningar döljs och inte blockerar
 * fordonet testas mot en riktig databas i {@code BookableBookingsIntegrationTest}.
 */
public class BookingServiceTest {

    private static final int VEHICLE_ID = 1;
    private static final int MECHANIC_ID = 2;
    private static final LocalDate DATE = LocalDate.of(2026, 6, 5);
    private static final LocalTime NINE = LocalTime.of(9, 0);

    private BookingRepository bookingRepository;
    private ServiceItemRepository serviceItemRepository;
    private MechanicRepository mechanicRepository;
    private VehicleRepo vehicleRepo;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        serviceItemRepository = mock(ServiceItemRepository.class);
        mechanicRepository = mock(MechanicRepository.class);
        vehicleRepo = mock(VehicleRepo.class);

        bookingService = new BookingService(
                bookingRepository,
                serviceItemRepository,
                mechanicRepository,
                vehicleRepo
        );

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        // Uppslagningen är default-metoder på repositoryt; mocken ska köra dem mot stubbade findById.
        when(serviceItemRepository.requireById(anyInt())).thenCallRealMethod();
        when(serviceItemRepository.requireAllById(any())).thenCallRealMethod();
    }

    // ----- create -----

    @Test
    @DisplayName("create avvisar ett fordon som redan är bokat samma dag och sparar inget")
    void createNewBooking_shouldThrowValidationError_WhenVehicleAlreadyBookedSameDay() {
        vehicleExists(VEHICLE_ID, "ABC123");
        mechanicExists(MECHANIC_ID, "Kalle");
        catalog(service(3, "Oil change", "899", 60));
        when(bookingRepository.existsByVehicleIdAndDateAndIdNotAndStatusNot(VEHICLE_ID, DATE, 0, BookingStatus.CANCELLED)).thenReturn(true);

        ValidationException ex = assertThrows(ValidationException.class, () ->
                bookingService.create(VEHICLE_ID, MECHANIC_ID, DATE, NINE, "Oil change",
                        Collections.singletonList(3)));

        assertEquals("error.vehicleBooked", ex.getMessageKey());
        assertEquals("ABC123", ex.getMessageArgs()[0]);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("create avvisar ett okänt fordon och sparar inget")
    void create_shouldThrow_WhenVehicleIsUnknown() {
        mechanicExists(MECHANIC_ID, "Kalle");
        catalog(service(3, "Oil change", "899", 60));

        assertThrows(EntityNotFoundException.class, () ->
                bookingService.create(99, MECHANIC_ID, DATE, NINE, "Oil change",
                        Collections.singletonList(3)));

        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("create avvisar en okänd mekaniker och sparar inget")
    void create_shouldThrow_WhenMechanicIsUnknown() {
        vehicleExists(VEHICLE_ID, "ABC123");
        catalog(service(3, "Oil change", "899", 60));

        assertThrows(EntityNotFoundException.class, () ->
                bookingService.create(VEHICLE_ID, 99, DATE, NINE, "Oil change",
                        Collections.singletonList(3)));

        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("create utan föreslagen mekaniker slår inte upp någon mekaniker och sparar bokningen")
    void create_withoutSuggestedMechanic_skipsMechanicLookup() {
        vehicleExists(VEHICLE_ID, "ABC123");
        catalog(service(3, "Oil change", "899", 60));

        Booking created = bookingService.create(VEHICLE_ID, null, DATE, NINE, "Oil change",
                Collections.singletonList(3));

        assertNull(created.getMechanicId());
        verifyNoInteractions(mechanicRepository);
        verify(bookingRepository).save(created);
    }

    // ----- update -----

    @Test
    @DisplayName("update avvisar ett fordon som har en annan bokning samma dag och sparar inget")
    void updateBooking_shouldThrowValidationError_WhenVehicleAlreadyBookedSameDay() {
        vehicleExists(VEHICLE_ID, "ABC123");
        mechanicExists(MECHANIC_ID, "Kalle");
        ServiceItem oilChange = service(3, "Oil change", "899", 60);
        catalog(oilChange);
        Booking existing = persisted(10, new Booking(VEHICLE_ID, MECHANIC_ID, DATE.minusDays(1), NINE,
                "Old description", Collections.singletonList(oilChange)));
        when(bookingRepository.findById(10)).thenReturn(Optional.of(existing));
        when(bookingRepository.existsByVehicleIdAndDateAndIdNotAndStatusNot(VEHICLE_ID, DATE, 10, BookingStatus.CANCELLED)).thenReturn(true);

        ValidationException ex = assertThrows(ValidationException.class, () ->
                bookingService.update(10, VEHICLE_ID, MECHANIC_ID, DATE, NINE,
                        "Oil change", Collections.singletonList(3)));

        assertEquals("error.vehicleBooked", ex.getMessageKey());
        assertEquals("ABC123", ex.getMessageArgs()[0]);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("update lyckas när datumet är oförändrat och ingen annan bokning krockar")
    void updateBooking_shouldSucceed_WhenDateIsUnchangedAndNoOtherBookingConflicts() {
        vehicleExists(VEHICLE_ID, "ABC123");
        mechanicExists(MECHANIC_ID, "Kalle");
        ServiceItem oilChange = service(3, "Oil change", "899", 60);
        catalog(oilChange);
        Booking existing = persisted(10, new Booking(VEHICLE_ID, MECHANIC_ID, DATE, NINE,
                "Old description", Collections.singletonList(oilChange)));
        when(bookingRepository.findById(10)).thenReturn(Optional.of(existing));

        Booking updated = assertDoesNotThrow(() ->
                bookingService.update(10, VEHICLE_ID, MECHANIC_ID, DATE, NINE,
                        "New description", Collections.singletonList(3)));

        assertEquals("New description", updated.getDescription());
        assertEquals(DATE, updated.getDate());
        verify(bookingRepository).existsByVehicleIdAndDateAndIdNotAndStatusNot(VEHICLE_ID, DATE, 10, BookingStatus.CANCELLED);
        verify(bookingRepository).save(existing);
    }

    // ----- createFromPrevious -----

    @Test
    @DisplayName("createFromPrevious avvisar ett fordon som redan är bokat samma dag och sparar inget")
    void createFromPreviousBooking_shouldThrowValidationError_WhenVehicleAlreadyBookedSameDay() {
        vehicleExists(VEHICLE_ID, "ABC123");
        mechanicExists(MECHANIC_ID, "Kalle");
        ServiceItem oilChange = service(3, "Oil change", "899", 60);
        catalog(oilChange);
        Booking previous = persisted(10, new Booking(VEHICLE_ID, MECHANIC_ID, LocalDate.of(2026, 5, 1),
                LocalTime.of(10, 0), "Old", Collections.singletonList(oilChange)));
        when(bookingRepository.findById(10)).thenReturn(Optional.of(previous));
        when(bookingRepository.existsByVehicleIdAndDateAndIdNotAndStatusNot(VEHICLE_ID, DATE, 0, BookingStatus.CANCELLED)).thenReturn(true);

        assertThrows(ValidationException.class, () ->
                bookingService.createFromPrevious(10, DATE, NINE, null, null, null, null));

        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("En ren kopia, även av en avbokad bokning (ångra avbokning), får dagens priser och samma fordon, mekaniker och beskrivning")
    void createFromPrevious_pureCopyOfCancelledBookingUsesTodaysPrices() {
        vehicleExists(VEHICLE_ID, "ABC123");
        mechanicExists(MECHANIC_ID, "Kalle");
        ServiceItem oilChange = service(1, "Oil change", "899", 45);
        ServiceItem brakes = service(2, "Brakes", "1295", 60);
        Booking previous = persisted(10, new Booking(VEHICLE_ID, MECHANIC_ID, LocalDate.of(2026, 5, 1),
                LocalTime.of(8, 0), "Old", Arrays.asList(oilChange, brakes)));
        previous.cancel();
        when(bookingRepository.findById(10)).thenReturn(Optional.of(previous));
        when(oilChange.getPrice()).thenReturn(Money.of(999));
        catalog(oilChange, brakes);

        BookingCopyResult result = bookingService.createFromPrevious(10, DATE, NINE, null, null, null, null);
        Booking copy = result.getBooking();

        assertMoney(Money.of(999), copy.getItems().get(0).getPriceAtBooking());
        assertMoney(Money.of(2294), copy.getTotalAgreedPrice());
        assertEquals(VEHICLE_ID, copy.getVehicleId());
        assertEquals(MECHANIC_ID, copy.getMechanicId());
        assertEquals("Old", copy.getDescription());
        assertEquals(DATE, copy.getDate());
        assertEquals(LocalTime.of(10, 45), copy.getEndTime());
        assertEquals(BookingStatus.BOOKED, copy.getStatus());
        assertEquals(Collections.singletonList(1), changedServiceIds(result));
        assertMoney(Money.of(899), previous.getItems().get(0).getPriceAtBooking());
        assertEquals(BookingStatus.CANCELLED, previous.getStatus());
        verify(bookingRepository).existsByVehicleIdAndDateAndIdNotAndStatusNot(
                VEHICLE_ID, DATE, 0, BookingStatus.CANCELLED);
        verify(bookingRepository).save(copy);
    }

    @Test
    @DisplayName("createFromPrevious rapporterar prisändringar bara för de tjänster som faktiskt väljs")
    void createFromPrevious_reportsPriceChangesOnlyForChosenServices() {
        vehicleExists(VEHICLE_ID, "ABC123");
        mechanicExists(MECHANIC_ID, "Kalle");
        ServiceItem oilChange = service(1, "Oil change", "899", 45);
        ServiceItem brakes = service(2, "Brakes", "1295", 60);
        ServiceItem diagnostics = service(3, "Diagnostics", "598.50", 60);
        Booking previous = persisted(10, new Booking(VEHICLE_ID, MECHANIC_ID, LocalDate.of(2026, 5, 1),
                LocalTime.of(8, 0), "Old", Arrays.asList(oilChange, brakes)));
        when(bookingRepository.findById(10)).thenReturn(Optional.of(previous));
        when(oilChange.getPrice()).thenReturn(Money.of(999));
        when(brakes.getPrice()).thenReturn(Money.of(1395));
        catalog(oilChange, brakes, diagnostics);

        BookingCopyResult result = bookingService.createFromPrevious(10, DATE, NINE, null, null, null,
                Arrays.asList(1, 3));

        assertEquals(Arrays.asList(1, 3), serviceIds(result.getBooking()));
        assertEquals(Collections.singletonList(1), changedServiceIds(result));
        BookingPriceChange change = result.getPriceChanges().get(0);
        assertMoney(Money.of(899), change.getPreviousPrice());
        assertMoney(Money.of(999), change.getCurrentPrice());
    }

    @Test
    @DisplayName("createFromPrevious räknar sluttid från det nya urvalet, inte originalets längd")
    void createFromPrevious_usesChosenServicesForEndTime() {
        vehicleExists(VEHICLE_ID, "ABC123");
        mechanicExists(MECHANIC_ID, "Kalle");
        ServiceItem threeHours = service(1, "Engine work", "3000", 180);
        ServiceItem oneHour = service(2, "Brakes", "1295", 60);
        catalog(threeHours, oneHour);
        Booking previous = persisted(10, new Booking(VEHICLE_ID, MECHANIC_ID, LocalDate.of(2026, 5, 1),
                LocalTime.of(8, 0), "Old", Collections.singletonList(threeHours)));
        when(bookingRepository.findById(10)).thenReturn(Optional.of(previous));

        BookingCopyResult result = bookingService.createFromPrevious(10, DATE, LocalTime.of(22, 0),
                null, null, null, Collections.singletonList(2));

        assertEquals(LocalTime.of(23, 0), result.getBooking().getEndTime());
        verify(bookingRepository).save(result.getBooking());
    }

    @Test
    @DisplayName("createFromPrevious avvisar ett okänt fordon som ersätter originalets och sparar inget")
    void createFromPrevious_shouldThrow_WhenOverrideVehicleIsUnknown() {
        vehicleExists(VEHICLE_ID, "ABC123");
        mechanicExists(MECHANIC_ID, "Kalle");
        ServiceItem oilChange = service(1, "Oil change", "899", 45);
        catalog(oilChange);
        Booking previous = persisted(10, new Booking(VEHICLE_ID, MECHANIC_ID, LocalDate.of(2026, 5, 1),
                LocalTime.of(8, 0), "Old", Collections.singletonList(oilChange)));
        when(bookingRepository.findById(10)).thenReturn(Optional.of(previous));

        assertThrows(EntityNotFoundException.class, () ->
                bookingService.createFromPrevious(10, DATE, NINE, null, 99, null, null));

        verify(bookingRepository, never()).save(any());
    }

    // ----- cancel -----

    @Test
    @DisplayName("cancel avbokar bokningen och sparar den")
    void cancel_setsCancelledAndSaves() {
        ServiceItem oilChange = service(1, "Oil change", "899", 45);
        Booking booking = persisted(10, new Booking(VEHICLE_ID, MECHANIC_ID, DATE, NINE,
                "Service", Collections.singletonList(oilChange)));
        when(bookingRepository.findById(10)).thenReturn(Optional.of(booking));

        Booking cancelled = bookingService.cancel(10);

        assertSame(booking, cancelled);
        assertEquals(BookingStatus.CANCELLED, cancelled.getStatus());
        verify(bookingRepository).save(booking);
    }

    @Test
    @DisplayName("cancel med okänt id kastar error.bookingNotFound och sparar inget")
    void cancel_unknownId_throwsAndDoesNotSave() {
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> bookingService.cancel(99));

        assertEquals("error.bookingNotFound", ex.getMessageKey());
        verify(bookingRepository, never()).save(any());
    }

    // ----- followWorkOrder -----

    @Test
    @DisplayName("followWorkOrder flyttar bokningen till orderns datum, tid och tjänster och sparar den")
    void followWorkOrder_appliesPlanAndSaves() {
        vehicleExists(VEHICLE_ID, "ABC123");
        ServiceItem oilChange = service(1, "Oil change", "899", 45);
        ServiceItem brakes = service(2, "Brakes", "1295", 60);
        catalog(oilChange, brakes);
        Booking booking = persisted(10, new Booking(VEHICLE_ID, MECHANIC_ID, DATE, NINE,
                "Service", Collections.singletonList(oilChange)));
        booking.followWorkOrder(WorkOrderStatus.CONFIRMED);
        when(bookingRepository.findById(10)).thenReturn(Optional.of(booking));
        LocalDate newDate = DATE.plusDays(1);

        bookingService.followWorkOrder(10, newDate, LocalTime.of(13, 0), Arrays.asList(1, 2));

        assertEquals(newDate, booking.getDate());
        assertEquals(LocalTime.of(14, 45), booking.getEndTime());
        verify(bookingRepository).existsByVehicleIdAndDateAndIdNotAndStatusNot(
                VEHICLE_ID, newDate, 10, BookingStatus.CANCELLED);
        verify(bookingRepository).save(booking);
    }

    @Test
    @DisplayName("followWorkOrder avvisar ett fordon som har en annan bokning den dagen och sparar inget")
    void followWorkOrder_rejectsVehicleBookedThatDay() {
        vehicleExists(VEHICLE_ID, "ABC123");
        ServiceItem oilChange = service(1, "Oil change", "899", 45);
        catalog(oilChange);
        Booking booking = persisted(10, new Booking(VEHICLE_ID, MECHANIC_ID, DATE, NINE,
                "Service", Collections.singletonList(oilChange)));
        booking.followWorkOrder(WorkOrderStatus.DRAFT);
        when(bookingRepository.findById(10)).thenReturn(Optional.of(booking));
        LocalDate newDate = DATE.plusDays(1);
        when(bookingRepository.existsByVehicleIdAndDateAndIdNotAndStatusNot(
                VEHICLE_ID, newDate, 10, BookingStatus.CANCELLED)).thenReturn(true);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> bookingService.followWorkOrder(10, newDate, NINE, Collections.singletonList(1)));

        assertEquals("error.vehicleBooked", ex.getMessageKey());
        verify(bookingRepository, never()).save(any());
    }

    // ----- followWorkOrderStatus -----

    @Test
    @DisplayName("followWorkOrderStatus med okänd bokning kastar error.bookingNotFound, som när planen följer ordern")
    void followWorkOrderStatus_unknownBooking_throwsLikeFollowWorkOrder() {
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> bookingService.followWorkOrderStatus(99, WorkOrderStatus.IN_PROGRESS));

        assertEquals("error.bookingNotFound", ex.getMessageKey());
        verify(bookingRepository, never()).save(any());
    }

    // ----- Hjälpmetoder -----

    private static ServiceItem service(int id, String name, String price, int minutes) {
        ServiceItem service = mock(ServiceItem.class);
        when(service.getId()).thenReturn(id);
        when(service.getName()).thenReturn(name);
        when(service.getPrice()).thenReturn(Money.of(price));
        when(service.getEstimatedMinutes()).thenReturn(minutes);
        return service;
    }

    private void catalog(ServiceItem... services) {
        for (ServiceItem service : services) {
            when(serviceItemRepository.findById(service.getId())).thenReturn(Optional.of(service));
        }
        when(serviceItemRepository.findAll()).thenReturn(Arrays.asList(services));
    }

    private void vehicleExists(int id, String registration) {
        Vehicle vehicle = mock(Vehicle.class);
        when(vehicle.getId()).thenReturn(id);
        when(vehicle.getRegistrationNumber()).thenReturn(registration);
        when(vehicleRepo.findById(id)).thenReturn(Optional.of(vehicle));
    }

    private void mechanicExists(int id, String name) {
        Mechanic mechanic = mock(Mechanic.class);
        when(mechanic.getId()).thenReturn(id);
        when(mechanic.getName()).thenReturn(name);
        when(mechanicRepository.findById(id)).thenReturn(Optional.of(mechanic));
    }

    // Ger bokningen ett id, som om den hade sparats i databasen.
    private static Booking persisted(int id, Booking booking) {
        ReflectionTestUtils.setField(booking, "id", id);
        return booking;
    }

    private static List<Integer> serviceIds(Booking booking) {
        return booking.getItems().stream()
                .map(item -> item.getServiceItemId())
                .collect(Collectors.toList());
    }

    private static List<Integer> changedServiceIds(BookingCopyResult result) {
        return result.getPriceChanges().stream()
                .map(BookingPriceChange::getServiceItemId)
                .collect(Collectors.toList());
    }
}
