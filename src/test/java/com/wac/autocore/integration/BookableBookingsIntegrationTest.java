package com.wac.autocore.integration;

import com.wac.autocore.AutoCoreConfig;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.BookingStatus;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.repository.BookingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>BookableBookingsIntegrationTest</b>
 * <p>Ansvar: Bevisar att {@link BookingRepository#findBookable(BookingStatus, WorkOrderStatus)}mot en riktig SQLite-fil
 * returnerar bokningar som är {@code BOOKED} och som inte har någon aktiv arbetsorder.</p>
 * <p>Rader skrivs med SQL och inte via servicen, så att testet efterliknar data som redan
 * fanns i databasen och inte bara går tur och retur genom den egna koden.</p>
 */
@SpringBootTest(classes = AutoCoreConfig.class)
@ActiveProfiles("test")
@DirtiesContext
class BookableBookingsIntegrationTest {

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

    private List<Integer> findBookableIds() {
        return bookingRepository.findBookable(BookingStatus.BOOKED, WorkOrderStatus.CANCELLED)
                .stream()
                .map(Booking::getId)
                .collect(Collectors.toList());
    }

    /* Skapar en bokning direkt i tabellen. vehicle_id och mechanic_id är NOT NULL,
     * så testet skapar först en kund, ett fordon och en mekaniker. */
    private int insertBooking(BookingStatus status) {
        int customerId = insertCustomer();
        int vehicleId = insertVehicle(customerId);
        int mechanicId = insertMechanic();

        jdbcTemplate.update(
                "INSERT INTO booking (vehicle_id, mechanic_id, date, start_time, end_time, description, status) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                vehicleId, mechanicId, "2026-11-03", "08:00", "09:00", "Test booking", status.name());
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
                "Test Kund", "070-0000000", "test@example.se", 0);
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
                "Test Mekaniker", "070-0000001", "General service");
        Integer id = jdbcTemplate.queryForObject("SELECT MAX(id) FROM mechanic", Integer.class);
        assertNotNull(id, "Insert should have produced a mechanic id");
        return id;
    }
}