package com.wac.autocore.integration;

import com.wac.autocore.AutoCoreConfig;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Invoice;
import com.wac.autocore.model.Payment;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.value.Money;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.CustomerRepo;
import com.wac.autocore.repository.InvoiceRepo;
import com.wac.autocore.repository.PaymentRepo;
import com.wac.autocore.repository.VehicleRepo;
import com.wac.autocore.repository.WorkOrderRepository;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.flywaydb.core.api.MigrationType;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static com.wac.autocore.testsupport.MoneyAssert.assertMoney;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <b>Fixture35DataPreservationIntegrationTest</b>
 * <p>Ansvar: Bevisar att data som skrivits av version 3.5 fortfarande går att läsa efter
 * plattformsbytet. Testet startar kontexten mot en kopia av en riktig 3.5-databas och jämför
 * belopp och datum med värden som lästs direkt ur filen med sqlite3, inte via koden som testas.</p>
 * <p>Originalfilen kopieras alltid till en temporär katalog och rörs aldrig. Checksumman kontrolleras
 * före och efter, så att ett test som råkar skriva till originalet blir rött.</p>
 * <p>Fixturen skapades från main efter PR #136 och får aldrig återskapas från en nyare version.</p>
 */
@SpringBootTest(classes = AutoCoreConfig.class)
@ActiveProfiles("test")
@DirtiesContext
@Transactional
class Fixture35DataPreservationIntegrationTest {

    private static final String FIXTURE = "/fixtures/autocore-3.5.db";

    private static final String FIXTURE_SHA256 = "e06d3e92ceed28656c84f79c032821d440947288f720e1825653346fbf181b7c";

    // Facit: läses ur fixturen med sqlite3, aldrig ur appen
    private static final int CUSTOMER_COUNT = 3;
    private static final int VEHICLE_COUNT = 3;
    private static final int BOOKING_COUNT = 3;
    private static final int WORK_ORDER_COUNT = 5;
    private static final int INVOICE_COUNT = 3;
    private static final int PAYMENT_COUNT = 2;

    // En faktura med rabatt WELCOME10 från headless-scenariot
    private static final int DISCOUNTED_INVOICE_ID = 3;
    private static final String DISCOUNTED_AMOUNT = "3790.00";
    private static final String DISCOUNTED_DISCOUNT = "379.00";
    private static final String DISCOUNTED_TOTAL = "3411.00";
    private static final LocalDate DISCOUNTED_INVOICE_DATE = LocalDate.of(2026, 10, 20);
    private static final int DISCOUNTED_LINE_COUNT = 2;

    // En bokning med sluttid som inte är jämn timme
    private static final int BOOKING_ID = 1;
    private static final LocalDate BOOKING_DATE = LocalDate.of(2026, 9, 21);
    private static final LocalTime BOOKING_START = LocalTime.of(8, 0);
    private static final LocalTime BOOKING_END = LocalTime.of(10, 45);

    // En planerad arbetsorder
    private static final int PLANNED_WORK_ORDER_ID = 3;
    private static final LocalDate PLANNED_DATE = LocalDate.of(2026, 9, 28);
    private static final LocalTime PLANNED_START = LocalTime.of(9, 0);

    // Betalningen av den rabatterade fakturan
    private static final int PAYMENT_ID = 2;
    private static final String PAYMENT_AMOUNT = "3411.00";
    private static final LocalDateTime PAYMENT_DATE = LocalDateTime.of(2026, 10, 20, 16, 50);

    @TempDir
    static Path tempDir;

    private static Path fixtureCopy;

    @Autowired private Flyway flyway;
    @Autowired private CustomerRepo customerRepo;
    @Autowired private VehicleRepo vehicleRepo;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private WorkOrderRepository workOrderRepository;
    @Autowired private InvoiceRepo invoiceRepo;
    @Autowired private PaymentRepo paymentRepo;

    /* Värdet hämtas först när kontexten byggs. Då är tempDir satt, och kopian skapas innan
     * Hikari och Flyway öppnar databasen. */
    @DynamicPropertySource
    static void sqliteFile(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:sqlite:" + copyFixture().toAbsolutePath());
    }

    private static synchronized Path copyFixture() {
        if (fixtureCopy == null) {
            Path target = tempDir.resolve("autocore-3.5-copy.db");
            try (InputStream in = openFixture()) {
                Files.copy(in, target);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not copy fixture " + FIXTURE, e);
            }
            fixtureCopy = target;
        }
        return fixtureCopy;
    }

    @BeforeAll
    static void fixtureIsUntouchedBefore() {
        assertFixtureChecksum("before the test");
    }

    @AfterAll
    static void fixtureIsUntouchedAfter() {
        assertFixtureChecksum("after the test");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("Flyway has applied every migration on top of the 3.5 history")
    void flywayIsAtLatestVersion() {
        MigrationInfo current = flyway.info().current();
        assertNotNull(current, "Flyway should report a current migration");

        MigrationVersion latest = MigrationVersion.EMPTY;
        for (MigrationInfo info : flyway.info().all()) {
            if (info.getVersion() != null && info.getVersion().compareTo(latest) > 0) {
                latest = info.getVersion();
            }
        }

        assertEquals(latest, current.getVersion(), "Current version should be the latest migration");
        assertEquals(MigrationState.SUCCESS, current.getState());
        assertEquals(0, flyway.info().pending().length, "No migrations should be pending");

        for (MigrationInfo applied : flyway.info().applied()) {
            assertTrue(applied.getType() != MigrationType.BASELINE,
                    "Fixture should be a real 3.5 database, not a baselined one");
        }
    }

    @Test
    @DisplayName("All 3.5 rows can be read through JPA")
    void allRowsCanBeRead() {
        assertEquals(CUSTOMER_COUNT, customerRepo.findAll().size(), "customers");
        assertEquals(VEHICLE_COUNT, vehicleRepo.findAll().size(), "vehicles");
        assertEquals(BOOKING_COUNT, bookingRepository.findAll().size(), "bookings");
        assertEquals(WORK_ORDER_COUNT, workOrderRepository.findAll().size(), "work orders");
        assertEquals(INVOICE_COUNT, invoiceRepo.findAll().size(), "invoices");
        assertEquals(PAYMENT_COUNT, paymentRepo.findAll().size(), "payments");
    }

    @Test
    @DisplayName("A discounted 3.5 invoice keeps its amounts, date and lines")
    void discountedInvoiceKeepsItsValues() {
        Invoice invoice = invoiceRepo.findById(DISCOUNTED_INVOICE_ID)
                .orElseThrow(() -> new AssertionError("Invoice " + DISCOUNTED_INVOICE_ID + " should exist"));

        assertMoney(Money.of(DISCOUNTED_AMOUNT), invoice.getAmount(), "amount");
        assertMoney(Money.of(DISCOUNTED_DISCOUNT), invoice.getDiscount(), "discount");
        assertMoney(Money.of(DISCOUNTED_TOTAL), invoice.getTotalAmount(), "total");
        assertEquals(DISCOUNTED_INVOICE_DATE, invoice.getInvoiceDate());
        assertEquals(DISCOUNTED_LINE_COUNT, invoice.getLines().size(), "invoice lines");
    }

    @Test
    @DisplayName("A 3.5 booking keeps its date and times")
    void bookingKeepsDateAndTimes() {
        Booking booking = bookingRepository.findById(BOOKING_ID)
                .orElseThrow(() -> new AssertionError("Booking " + BOOKING_ID + " should exist"));

        assertEquals(BOOKING_DATE, booking.getDate());
        assertEquals(BOOKING_START, booking.getStartTime());
        assertEquals(BOOKING_END, booking.getEndTime());
    }

    @Test
    @DisplayName("A planned 3.5 work order keeps its planned date and start time")
    void plannedWorkOrderKeepsItsSchedule() {
        WorkOrder order = workOrderRepository.findById(PLANNED_WORK_ORDER_ID)
                .orElseThrow(() -> new AssertionError("Work order " + PLANNED_WORK_ORDER_ID + " should exist"));

        assertEquals(PLANNED_DATE, order.getPlannedDate());
        assertEquals(PLANNED_START, order.getPlannedStartTime());
    }

    @Test
    @DisplayName("A 3.5 payment keeps its amount and timestamp")
    void paymentKeepsItsValues() {
        Payment payment = paymentRepo.findById(PAYMENT_ID)
                .orElseThrow(() -> new AssertionError("Payment " + PAYMENT_ID + " should exist"));

        assertMoney(Money.of(PAYMENT_AMOUNT), payment.getAmount(), "payment amount");
        assertEquals(PAYMENT_DATE, payment.getPaymentDate());
    }

    private static void assertFixtureChecksum(String when) {
        String actual = sha256OfFixture();
        if (!FIXTURE_SHA256.equals(actual)) {
            fail("Fixture " + FIXTURE + " has unexpected checksum " + when
                    + ". Expected " + FIXTURE_SHA256 + " but was " + actual
                    + ". If the fixture was replaced on purpose, update FIXTURE_SHA256.");
        }
    }

    private static String sha256OfFixture() {
        try (InputStream in = openFixture()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream dis = new DigestInputStream(in, digest)) {
                byte[] buffer = new byte[8192];
                while (dis.read(buffer) != -1) {
                    // läser bara igenom filen så att digesten uppdateras
                }
            }
            StringBuilder hex = new StringBuilder();
            for (byte b : digest.digest()) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read fixture " + FIXTURE, e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 should always be available", e);
        }
    }

    private static InputStream openFixture() {
        InputStream in = Fixture35DataPreservationIntegrationTest.class.getResourceAsStream(FIXTURE);
        if (in == null) {
            throw new IllegalStateException("Fixture " + FIXTURE + " is missing from the test classpath");
        }
        return in;
    }
}
