package com.wac.autocore.service;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.BookingServiceItem;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.MechanicRepository;
import com.wac.autocore.repository.ServiceItemRepository;
import com.wac.autocore.repository.VehicleRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        when(oilChange.getPrice()).thenReturn(999.0);
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
        when(oldOilChange.getPrice()).thenReturn(899.0);
        when(oldOilChange.getEstimatedMinutes()).thenReturn(60);

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
        assertEquals(899.0, updated.getItems().get(0).getPriceAtBooking());
    }
}
