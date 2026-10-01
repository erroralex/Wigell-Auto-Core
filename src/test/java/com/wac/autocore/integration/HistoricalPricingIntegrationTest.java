package com.wac.autocore.integration;

import com.wac.autocore.AutoCoreConfig;
import com.wac.autocore.model.*;
import com.wac.autocore.repository.*;
import com.wac.autocore.service.BookingService;
import com.wac.autocore.service.InvoiceService;
import com.wac.autocore.service.ServiceItemService;
import com.wac.autocore.service.WorkOrderService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * <b>HistoricalPricingIntegrationTest</b>
 * <p>Ansvar: Bevisar att en ändring i tjänstekatalogen aldrig skriver om historiska priser
 * på bokningar, arbetsordrar eller fakturor.</p>
 * <p>Testerna kör mot en riktig SQLite-fil som Flyway bygger upp, inte mot mockar.
 * Testmetoderna är inte {@code @Transactional}: varje service- och repository-anrop får
 * ett eget persistence context, så allt som läses tillbaka kommer från databasfilen och
 * inte från Hibernates cache.</p>
 */
@SpringBootTest(classes = AutoCoreConfig.class)
@ActiveProfiles("test")
@DirtiesContext
public class HistoricalPricingIntegrationTest {

    private static final double ORIGINAL_PRICE = 899.0;
    private static final double UPDATED_PRICE = 999.0;
    private static final LocalDate BOOKING_DATE = LocalDate.of(2026, 10,10);

    private static final AtomicInteger REG_SEQUENCE = new AtomicInteger(100);

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void sqliteFile(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> jdbcUrl(tempDir.resolve("historical-pricing.db")));
    }

    @Autowired
    private Flyway flyway;
    @Autowired private ServiceItemService serviceItemService;
    @Autowired private ServiceItemRepository serviceItemRepository;
    @Autowired private BookingService bookingService;
    @Autowired private WorkOrderService workOrderService;
    @Autowired private WorkOrderRepository workOrderRepository;
    @Autowired private InvoiceService invoiceService;
    @Autowired private InvoiceRepo invoiceRepo;
    @Autowired private CustomerRepo customerRepo;
    @Autowired private MechanicRepository mechanicRepository;
    @Autowired private VehicleRepo vehicleRepo;

    @Test
    @DisplayName("Context is starting and Flyway has migrated the test database")
    void contextLoads() {
        assertNotNull(flyway.info().current(), "Flyway should run at least one migration");
    }

    @Test
    @DisplayName("Invoice and work order keeps 899:- pricing when price is raised to 999:-")
    void priceChangeDoesNotRewriteHistoricalRecords() {

        //Arrange: Oct 10: Oilchange costs 899:-
        PricingScenario scenario = PricingScenario.create(
                serviceItemRepository,
                customerRepo,
                mechanicRepository,
                vehicleRepo,
                bookingService,
                workOrderService,
                invoiceService);

        // Act: Nov 1: Price is raised
        serviceItemService.updatePrice(scenario.serviceItemId, UPDATED_PRICE);

        // Assert: Catalogue price is updated
        assertEquals(UPDATED_PRICE,
                serviceItemRepository.findById(scenario.serviceItemId).get().getPrice(),
                "Catalogue price should be updated to 999.0:-");

        // Assert: History is unchanged when read from SQLite
        Booking booking = bookingService.findById(scenario.bookingId).get();
        assertEquals(ORIGINAL_PRICE, booking.getItems().get(0).getPriceAtBooking(),
                "Booking should keep price at booking-time");

        WorkOrder workOrder = workOrderRepository.findById(scenario.workOrderId).get();
        WorkOrderItem line = workOrder.getItems().get(0);
        assertEquals(ORIGINAL_PRICE, line.getAgreedPrice(),
                "Work order line should keep price at booking-time");
        assertEquals(ORIGINAL_PRICE, workOrder.getTotalPrice());

        Invoice invoice = invoiceRepo.findById(scenario.invoiceId).get();
        assertEquals(ORIGINAL_PRICE, invoice.getAmount(),
                "Invoice should be 899:- after price-raise");
        assertEquals(ORIGINAL_PRICE, invoice.getTotalAmount());
    }

    @Test
    @DisplayName("New invoice after price raise should still use work order snapshot")
    void invoiceCreatedAfterPriceChangeUsesSnapshot() {
        PricingScenario scenario = PricingScenario.createWithoutInvoice(
                serviceItemRepository,
                customerRepo,
                mechanicRepository,
                vehicleRepo,
                bookingService,
                workOrderService
        );

        serviceItemService.updatePrice(scenario.serviceItemId, UPDATED_PRICE);
        Invoice invoice = invoiceService.create(scenario.workOrderId, null);

        assertEquals(ORIGINAL_PRICE, invoiceRepo.findById(invoice.getId()).get().getAmount(),
                "Invoice should keep price at booking-time, not the new catalogue price");
    }

    @Test
    @DisplayName("Historical data survives application restart")
    void historicalDataSurvivesApplicationRestart() {
        String url = jdbcUrl(tempDir.resolve("restart.db"));
        PricingScenario scenario;

        // First run: Create everything, raise price and close application
        try (ConfigurableApplicationContext first = startApplication(url)) {
            scenario = PricingScenario.create(
                    first.getBean(ServiceItemRepository.class),
                    first.getBean(CustomerRepo.class),
                    first.getBean(MechanicRepository.class),
                    first.getBean(VehicleRepo.class),
                    first.getBean(BookingService.class),
                    first.getBean(WorkOrderService.class),
                    first.getBean(InvoiceService.class));

            first.getBean(ServiceItemService.class).updatePrice(scenario.serviceItemId, UPDATED_PRICE);
        }

        try (ConfigurableApplicationContext second = startApplication(url)) {
            ServiceItem serviceItem = second.getBean(ServiceItemRepository.class)
                    .findById(scenario.serviceItemId).get();
            Booking booking = second.getBean(BookingService.class)
                    .findById(scenario.bookingId).get();
            WorkOrder workOrder = second.getBean(WorkOrderRepository.class)
                    .findById(scenario.workOrderId).get();
            Invoice invoice = second.getBean(InvoiceRepo.class)
                    .findById(scenario.invoiceId).get();

            // Prices
            assertEquals(UPDATED_PRICE, serviceItem.getPrice());
            assertEquals(ORIGINAL_PRICE, booking.getItems().get(0).getPriceAtBooking());
            assertEquals(ORIGINAL_PRICE, workOrder.getItems().get(0).getAgreedPrice());
            assertEquals(ORIGINAL_PRICE, invoice.getAmount());

            // Relations
            assertEquals(scenario.workOrderId, invoice.getWorkOrderId());
            assertEquals(scenario.bookingId, workOrder.getBookingId());
            assertEquals(scenario.vehicleId, booking.getVehicleId());
            assertEquals(scenario.serviceItemId, workOrder.getItems().get(0).getServiceItemId());

            // Lines
            assertEquals(1, booking.getItems().size());
            assertEquals(1, workOrder.getItems().size());
            assertEquals("Oljebyte", workOrder.getItems().get(0).getServiceName());
        }
    }

    private static ConfigurableApplicationContext startApplication(String url) {
        return new SpringApplicationBuilder(AutoCoreConfig.class)
                .profiles("test")
                .run("--spring.datasource.url=" + url);
    }

    private static String jdbcUrl(Path dbFile) {
        return "jdbc:sqlite:" + dbFile.toAbsolutePath();
    }

    /**
     * Bygger kedjan tjänst -> kund -> fordon -> bokning -> arbetsorder -> faktura
     * och håller id:na. Allt skapas via service-lagret där det finns en metod för det,
     * så att testet går samma väg som applikationen.
     */
    private static final class PricingScenario {
        int serviceItemId;
        int vehicleId;
        int bookingId;
        int workOrderId;
        int invoiceId;

        static PricingScenario create(ServiceItemRepository serviceItems,
                                      CustomerRepo customers,
                                      MechanicRepository mechanics,
                                      VehicleRepo vehicles,
                                      BookingService bookings,
                                      WorkOrderService workOrders,
                                      InvoiceService invoices) {
            PricingScenario scenario = createWithoutInvoice(
                    serviceItems, customers, mechanics, vehicles, bookings, workOrders);
            scenario.invoiceId = invoices.create(scenario.workOrderId, null).getId();
            return scenario;
        }

        static PricingScenario createWithoutInvoice(ServiceItemRepository serviceItems,
                                                    CustomerRepo customers,
                                                    MechanicRepository mechanics,
                                                    VehicleRepo vehicles,
                                                    BookingService bookings,
                                                    WorkOrderService workOrders) {
            PricingScenario scenario = new PricingScenario();

            ServiceItem oilChange = serviceItems.save(
                    new ServiceItem("Oljebyte", "Motorolja och oljefilter", ORIGINAL_PRICE, 45));
            scenario.serviceItemId = oilChange.getId();

            // Icke-VIP-kund, så att ingen rabatt påverkar fakturans belopp
            Customer customer = customers.save(
                    new Customer("Test Kund", "070-0000000", "test@example.se"));
            Mechanic mechanic = mechanics.save(
                    new Mechanic("Test Mekaniker", "070-0000001", "General service"));
            Vehicle vehicle = vehicles.save(new Vehicle(
                    "TST" + REG_SEQUENCE.incrementAndGet(), "Volvo", "V60", 2020, customer.getId()));
            scenario.vehicleId = vehicle.getId();

            // Egen mekaniker per scenario, så att överlappskontrollen aldrig slår till
            Booking booking = bookings.create(
                    vehicle.getId(), mechanic.getId(), BOOKING_DATE, LocalTime.of(8, 0),
                    "Oljebyte", Collections.singletonList(oilChange.getId()));
            scenario.bookingId = booking.getId();

            scenario.workOrderId = workOrders.createWorkOrder(booking.getId()).getId();
            return scenario;
        }
    }
}
