package com.wac.autocore.service;

import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.*;
import com.wac.autocore.model.value.Money;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.MechanicRepository;
import com.wac.autocore.repository.ServiceItemRepository;
import com.wac.autocore.repository.VehicleRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cglib.core.Local;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;

import java.util.List;
import java.util.Optional;

import static com.wac.autocore.testsupport.MoneyAssert.assertMoney;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class BookingServiceTest {

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
    }

    @Test
    void updateShouldKeepPriceOfExistingService() {
        ServiceItem oilChange = mock(ServiceItem.class);

        when(oilChange.getId()).thenReturn(1);
        when(oilChange.getName()).thenReturn("Oil change");
        when(oilChange.getPrice()).thenReturn(Money.of(999));
        when(oilChange.getEstimatedMinutes()).thenReturn(60);

        Booking booking = new Booking(
                1,
                1,
                LocalDate.of(2026, 10, 7),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                "Test booking"
        );

        BookingServiceItem existingItem = BookingServiceItem.snapshotOf(oilChange);

        ServiceItem oldOilChange = mock(ServiceItem.class);
        when(oldOilChange.getId()).thenReturn(1);
        when(oldOilChange.getName()).thenReturn("Oil change");
        when(oldOilChange.getPrice()).thenReturn(Money.of(899));
        when(oldOilChange.getEstimatedMinutes()).thenReturn(60);

        when(vehicleRepo.existsById(booking.getVehicleId())).thenReturn(true);
        when(mechanicRepository.existsById(booking.getMechanicId())).thenReturn(true);

        Booking originalBooking = new Booking(
                1,
                1,
                LocalDate.of(2026, 10, 7),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                "Test booking"
        );

        originalBooking.addServiceItem(oldOilChange);

        when(bookingRepository.findById(1))
                .thenReturn(Optional.of(originalBooking));

        when(serviceItemRepository.findById(1))
                .thenReturn(Optional.of(oilChange));

        when(bookingRepository.findBookingByMechanicIdAndDate(
                1,
                LocalDate.of(2026, 10, 7)))
                .thenReturn(Collections.emptyList());

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Booking updated = bookingService.update(
                1,
                1,
                1,
                LocalDate.of(2026, 10, 7),
                LocalTime.of(10, 0),
                "Updated booking",
                Collections.singletonList(1)
        );

        assertEquals(1, updated.getItems().size());
        assertMoney(Money.of(899), updated.getItems().get(0).getPriceAtBooking());
    }

    @Test
    void createNewBooking_shouldThrowValidationError_WhenVehicleAlreadyBookedSameDay() {
        int vehicleId = 1;
        int mechanicId = 2;
        int serviceItemId = 3;
        LocalDate date = LocalDate.of(2026, 6, 5);
        LocalTime startTime = LocalTime.of(9,0);

        ServiceItem serviceItem = mock(ServiceItem.class);
        when(serviceItem.getEstimatedMinutes()).thenReturn(60);
        when(serviceItemRepository.findById(serviceItemId))
                .thenReturn(Optional.of(serviceItem));

        when(bookingRepository.existsByVehicleIdAndDate(vehicleId, date))
                .thenReturn(true);

        when(vehicleRepo.existsById(vehicleId)).thenReturn(true);
        when(mechanicRepository.existsById(mechanicId)).thenReturn(true);

        Vehicle vehicle = mock(Vehicle.class);
        when(vehicle.getRegistrationNumber()).thenReturn("ABC123");
        when(vehicleRepo.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(vehicleRepo.existsById(vehicleId)).thenReturn(true);
        when(mechanicRepository.existsById(mechanicId)).thenReturn(true);

        ValidationException ex = assertThrows(ValidationException.class, () ->
                bookingService.create(vehicleId, mechanicId, date, startTime, "Oil change",
                        Collections.singletonList(serviceItemId)));

        assertEquals("error.vehicleBooked", ex.getMessageKey());
        assertEquals("ABC123", ex.getMessageArgs()[0]);

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void updateBooking_shouldThrowValidationError_WhenVehicleAlreadyBookedSameDay() {
        int bookingId = 10;
        int vehicleId = 1;
        int mechanicId = 2;
        int serviceItemId = 3;
        LocalDate date = LocalDate.of(2026, 6, 5);
        LocalTime startTime = LocalTime.of(9,0);

        Booking existingBooking = mock(Booking.class);
        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(existingBooking));

        ServiceItem serviceItem = mock(ServiceItem.class);
        when(serviceItem.getEstimatedMinutes()).thenReturn(60);
        when(serviceItemRepository.findById(serviceItemId))
                .thenReturn(Optional.of(serviceItem));

        when(bookingRepository.existsByVehicleIdAndDateAndIdNot(vehicleId, date, bookingId))
                .thenReturn(true);

        when(vehicleRepo.existsById(vehicleId)).thenReturn(true);
        when(mechanicRepository.existsById(mechanicId)).thenReturn(true);
        when(existingBooking.getStatus()).thenReturn(BookingStatus.BOOKED);

        Vehicle vehicle = mock(Vehicle.class);
        when(vehicle.getRegistrationNumber()).thenReturn("ABC123");
        when(vehicleRepo.findById(vehicleId)).thenReturn(Optional.of(vehicle));

        ValidationException ex = assertThrows(ValidationException.class, () ->
                bookingService.update(bookingId, vehicleId, mechanicId, date, startTime,
                        "Oil change", Collections.singletonList(serviceItemId)));

        assertEquals("error.vehicleBooked", ex.getMessageKey());
        assertEquals("ABC123", ex.getMessageArgs()[0]);

        verify(bookingRepository, never()).existsByVehicleIdAndDate(anyInt(), any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createFromPreviousBooking_shouldThrowValidationError_WhenVehicleAlreadyBookedSameDay() {
        int previousBookingId = 10;
        int vehicleId = 1;
        int mechanicId = 2;
        LocalDate date = LocalDate.of(2026, 6, 5);
        LocalTime startTime = LocalTime.of(9, 0);

        ServiceItem oilChange = mock(ServiceItem.class);
        when(oilChange.getId()).thenReturn(3);
        when(oilChange.getName()).thenReturn("Oil change");
        when(oilChange.getPrice()).thenReturn(Money.of(899));
        when(oilChange.getEstimatedMinutes()).thenReturn(60);

        Booking previous = new Booking(vehicleId, mechanicId,
                LocalDate.of(2026, 5, 1), LocalTime.of(10, 0), LocalTime.of(11, 0), "Old");
        previous.addServiceItem(oilChange);

        when(bookingRepository.findById(previousBookingId)).thenReturn(Optional.of(previous));
        when(mechanicRepository.existsById(mechanicId)).thenReturn(true);
        when(serviceItemRepository.findById(3)).thenReturn(Optional.of(oilChange));
        when(bookingRepository.existsByVehicleIdAndDate(vehicleId, date)).thenReturn(true);

        Vehicle vehicle = mock(Vehicle.class);
        when(vehicle.getRegistrationNumber()).thenReturn("ABC123");
        when(vehicleRepo.findById(vehicleId)).thenReturn(Optional.of(vehicle));

        when(vehicleRepo.existsById(vehicleId)).thenReturn(true);


        assertThrows(ValidationException.class, () ->
                bookingService.createFromPrevious(previousBookingId, date, startTime, null, null, null, null));

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void updateBooking_shouldSucceed_WhenDateIsUnchangedAndNoOtherBookingConflicts() {
        int bookingId = 10;
        int vehicleId = 1;
        int mechanicId = 2;
        int serviceItemId = 3;
        LocalDate date = LocalDate.of(2026, 6, 5);
        LocalTime startTime = LocalTime.of(9, 0);

        ServiceItem oilChange = mock(ServiceItem.class);
        when(oilChange.getId()).thenReturn(serviceItemId);
        when(oilChange.getName()).thenReturn("Oil change");
        when(oilChange.getPrice()).thenReturn(Money.of(899));
        when(oilChange.getEstimatedMinutes()).thenReturn(60);

        when(vehicleRepo.existsById(vehicleId)).thenReturn(true);
        when(mechanicRepository.existsById(mechanicId)).thenReturn(true);

        Booking existing = new Booking(vehicleId, mechanicId, date,
                startTime, startTime.plusMinutes(60), "Old description");
        existing.addServiceItem(oilChange);

        when(vehicleRepo.existsById(vehicleId)).thenReturn(true);
        when(mechanicRepository.existsById(mechanicId)).thenReturn(true);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(existing));
        when(serviceItemRepository.findById(serviceItemId)).thenReturn(Optional.of(oilChange));

        when(bookingRepository.existsByVehicleIdAndDateAndIdNot(vehicleId, date, bookingId)).thenReturn(false);
        when(bookingRepository.findBookingByMechanicIdAndDate(mechanicId, date))
                .thenReturn(Collections.emptyList());
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Booking updated = assertDoesNotThrow(() ->
                bookingService.update(bookingId, vehicleId, mechanicId, date, startTime,
                        "New description", Collections.singletonList(serviceItemId)));

        assertEquals("New description", updated.getDescription());
        assertEquals(date, updated.getDate());

        verify(bookingRepository).existsByVehicleIdAndDateAndIdNot(vehicleId, date, bookingId);
        verify(bookingRepository, never()).existsByVehicleIdAndDate(anyInt(), any());
        verify(bookingRepository).save(existing);
    }
}
