package com.wac.autocore.integration;

import com.wac.autocore.AutoCoreConfig;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.BookingStatus;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.service.BookingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import com.wac.autocore.testsupport.WithAdmin;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>BookableBookingsIntegrationTest</b>
 * <p>Responsibility: Proves that {@link BookingRepository#findBookable(BookingStatus, WorkOrderStatus)} against a real SQLite file
 * returns bookings that are {@code BOOKED} and have no active work order, and that the vehicle check
 * and the listing of active bookings in {@link BookingService} ignore cancelled bookings.</p>
 * <p>Rows are written with SQL and not via the service, so the test mimics data that already
 * existed in the database and does not just make a round trip through our own code.</p>
 */
@SpringBootTest(classes = AutoCoreConfig.class)
@ActiveProfiles("test")
@WithAdmin
@DirtiesContext
class BookableBookingsIntegrationTest {

    private static final LocalDate BOOKING_DATE = LocalDate.of(2026, 11, 3);

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void sqliteFile(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:sqlite:" + tempDir.resolve("bookable-bookings.db").toAbsolutePath());
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private BookingService bookingService;

    @Test
    @DisplayName("BOOKED booking without any work order is bookable")
    void bookedWithoutWorkOrderIsBookable() {
        int bookingId = insertBooking(BookingStatus.BOOKED);

        List<Integer> ids = findBookableIds();

        assertTrue(ids.contains(bookingId),
                "BOOKED booking without work order should be bookable, got ids " + ids);
    }

    @Test
    @DisplayName("BOOKED booking with an active work order is not bookable")
    void bookedWithActiveWorkOrderIsNotBookable() {
        int bookingId = insertBooking(BookingStatus.BOOKED);
        insertWorkOrder(bookingId, WorkOrderStatus.CONFIRMED);

        List<Integer> ids = findBookableIds();

        assertTrue(!ids.contains(bookingId),
                "BOOKED booking with active work order should not be bookable, got ids " + ids);
    }

    @Test
    @DisplayName("Non-BOOKED booking is not bookable")
    void workOrderCreatedIsNotBookable() {
        int bookingId = insertBooking(BookingStatus.WORK_ORDER_CREATED);

        List<Integer> ids = findBookableIds();

        assertTrue(!ids.contains(bookingId),
                "WORK_ORDER_CREATED booking should not be bookable, got ids " + ids);
    }

    @Test
    @DisplayName("BOOKED booking with only a cancelled work order is bookable")
    void bookedWithCancelledWorkOrderIsBookable() {
        int bookingId = insertBooking(BookingStatus.BOOKED);
        insertWorkOrder(bookingId, WorkOrderStatus.CANCELLED);

        List<Integer> ids = findBookableIds();

        assertTrue(ids.contains(bookingId),
                "BOOKED booking with cancelled work order should be bookable, got ids " + ids);
    }

    // ----- Cancelled bookings -----

    @Test
    @DisplayName("Cancelled booking does not block its vehicle, an active booking does")
    void cancelledBookingDoesNotBlockVehicle() {
        int cancelledId = insertBooking(BookingStatus.CANCELLED);
        int cancelledVehicle = vehicleOf(cancelledId);
        int activeId = insertBooking(BookingStatus.BOOKED);
        int activeVehicle = vehicleOf(activeId);

        assertFalse(bookingService.isVehicleBooked(cancelledVehicle, BOOKING_DATE));
        assertFalse(bookingRepository.existsByVehicleIdAndDateAndIdNotAndStatusNot(
                cancelledVehicle, BOOKING_DATE, 0, BookingStatus.CANCELLED));
        assertTrue(bookingService.isVehicleBooked(activeVehicle, BOOKING_DATE));
        assertTrue(bookingRepository.existsByVehicleIdAndDateAndIdNotAndStatusNot(
                activeVehicle, BOOKING_DATE, 0, BookingStatus.CANCELLED));
        assertFalse(bookingRepository.existsByVehicleIdAndDateAndIdNotAndStatusNot(
                activeVehicle, BOOKING_DATE, activeId, BookingStatus.CANCELLED),
                "The booking itself should be excluded when it is updated");
    }

    @Test
    @DisplayName("listActive hides cancelled bookings, listAll shows them")
    void listActiveHidesCancelledBookings() {
        int cancelledId = insertBooking(BookingStatus.CANCELLED);
        int activeId = insertBooking(BookingStatus.BOOKED);

        List<Integer> ids = idsOf(bookingService.listActive());
        List<Integer> allIds = idsOf(bookingService.listAll());

        assertTrue(ids.contains(activeId), "Active booking should be listed, got ids " + ids);
        assertFalse(ids.contains(cancelledId), "Cancelled booking should be hidden, got ids " + ids);
        assertTrue(allIds.contains(cancelledId), "listAll should include cancelled bookings, got ids " + allIds);
    }

    private static List<Integer> idsOf(List<Booking> bookings) {
        return bookings.stream()
                .map(Booking::getId)
                .collect(Collectors.toList());
    }

    private int vehicleOf(int bookingId) {
        Integer vehicleId = jdbcTemplate.queryForObject(
                "SELECT vehicle_id FROM booking WHERE id = ?", Integer.class, bookingId);
        assertNotNull(vehicleId);
        return vehicleId;
    }

    private List<Integer> findBookableIds() {
        return idsOf(bookingRepository.findBookable(BookingStatus.BOOKED, WorkOrderStatus.CANCELLED));
    }

    /* Creates a booking directly in the table. vehicle_id and mechanic_id are NOT NULL,
     * so the test first creates a customer, a vehicle and a mechanic. */
    private int insertBooking(BookingStatus status) {
        int customerId = insertCustomer();
        int vehicleId = insertVehicle(customerId);
        int mechanicId = insertMechanic();

        jdbcTemplate.update(
                "INSERT INTO booking (vehicle_id, mechanic_id, date, start_time, end_time, description, status) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                vehicleId, mechanicId, BOOKING_DATE.toString(), "08:00", "09:00", "Test booking", status.name());
        Integer id = jdbcTemplate.queryForObject("SELECT MAX(id) FROM booking", Integer.class);
        assertNotNull(id, "Insert should have produced a booking id");
        return id;
    }

    private void insertWorkOrder(int bookingId, WorkOrderStatus status) {
        jdbcTemplate.update(
                "INSERT INTO work_order (booking_id, type, status) VALUES (?, ?, ?)",
                bookingId, "PLANNED", status.name());
    }

    private int insertCustomer() {
        jdbcTemplate.update(
                "INSERT INTO customer (name, phone, email, vip) VALUES (?, ?, ?, ?)",
                "Test Customer", "070-0000000", "test@example.se", 0);
        Integer id = jdbcTemplate.queryForObject("SELECT MAX(id) FROM customer", Integer.class);
        assertNotNull(id, "Insert should have produced a customer id");
        return id;
    }

    private int insertVehicle(int customerId) {
        jdbcTemplate.update(
                "INSERT INTO vehicle (registration_number, brand, model, year, customer_id) VALUES (?, ?, ?, ?, ?)",
                "TST" + System.nanoTime() % 100000, "Volvo", "V60", 2020, customerId);
        Integer id = jdbcTemplate.queryForObject("SELECT MAX(id) FROM vehicle", Integer.class);
        assertNotNull(id, "Insert should have produced a vehicle id");
        return id;
    }

    private int insertMechanic() {
        jdbcTemplate.update(
                "INSERT INTO mechanic (name, phone, specialization) VALUES (?, ?, ?)",
                "Test Mechanic", "070-0000001", "General service");
        Integer id = jdbcTemplate.queryForObject("SELECT MAX(id) FROM mechanic", Integer.class);
        assertNotNull(id, "Insert should have produced a mechanic id");
        return id;
    }
}