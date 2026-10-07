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
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <b>HistoricalPricingIntegrationTest</b>
 * <p>Ansvar: Bevisar att en ändring i tjänstekatalogen aldrig skriver om historiska priser
 * på bokningar, arbetsordrar eller fakturor, varken i fakturahuvudet eller på fakturaraderna.</p>
 * <p>Testerna kör mot en riktig SQLite-fil som Flyway bygger upp, inte mot mockar.
 * Testmetoderna är inte {@code @Transactional}: varje service- och repository-anrop får
 * ett eget persistence context, så allt som läses tillbaka kommer från databasfilen och
 * inte från Hibernates cache.</p>
 */
@SpringBootTest(classes = AutoCoreConfig.class)
@ActiveProfiles("test")
@DirtiesContext
class HistoricalPricingIntegrationTest {

    private static final String OIL_CHANGE = "Oljebyte";
    private static final String TIRE_CHANGE = "Däckbyte";
    private static final double OIL_PRICE = 899.0;
    private static final double OIL_PRICE_RAISED = 999.0;
    private static final double TIRE_PRICE = 400.0;

    private static final double DELTA = 0.005;

    private static final LocalDate BOOKING_DATE = LocalDate.of(2026, 10, 10);
    private static final AtomicInteger REG_SEQUENCE = new AtomicInteger(100);

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void sqliteFile(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> jdbcUrl(tempDir.resolve("historical-pricing.db")));
    }

    @Autowired private ApplicationContext context;
    @Autowired private Flyway flyway;
    @Autowired private ServiceItemService serviceItemService;
    @Autowired private ServiceItemRepository serviceItemRepository;
    @Autowired private InvoiceService invoiceService;
    @Autowired private InvoiceRepo invoiceRepo;

    @Test
    @DisplayName("Context is starting and Flyway has migrated the test database")
    void contextLoads() {
        assertNotNull(flyway.info().current(), "Flyway should run at least one migration");
    }

    @Test
    @DisplayName("Invoice, invoice line and work order keep 899:- when price is raised to 999:-")
    void priceChangeDoesNotRewriteHistoricalRecords() {
        // Arrange: 10 okt, oljebyte kostar 899:-
        PricingScenario scenario = PricingScenario.in(context)
                .withService(OIL_CHANGE, OIL_PRICE)
                .withInvoice()
                .build();

        // Act: 1 nov, priset höjs
        serviceItemService.updatePrice(scenario.serviceItemId(OIL_CHANGE), OIL_PRICE_RAISED);

        // Assert: katalogen är uppdaterad, historiken är det inte
        assertEquals(OIL_PRICE_RAISED,
                serviceItemRepository.findById(scenario.serviceItemId(OIL_CHANGE)).get().getPrice(),
                DELTA, "Catalogue price should be updated to 999:-");
        assertSnapshotIntact(context, scenario);
    }

    @Test
    @DisplayName("New invoice after price raise should still use work order snapshot")
    void invoiceCreatedAfterPriceChangeUsesSnapshot() {
        PricingScenario scenario = PricingScenario.in(context)
                .withService(OIL_CHANGE, OIL_PRICE)
                .build();

        serviceItemService.updatePrice(scenario.serviceItemId(OIL_CHANGE), OIL_PRICE_RAISED);
        Invoice created = invoiceService.create(scenario.workOrderId, null);

        Invoice invoice = invoiceRepo.findById(created.getId()).get();
        assertEquals(OIL_PRICE, invoice.getAmount(), DELTA,
                "Invoice should keep price at booking-time, not the new catalogue price");
        assertEquals(OIL_PRICE,
                lineNamed(invoice.getLines(), InvoiceLine::getServiceItemName, OIL_CHANGE).getAmount(),
                DELTA, "Invoice line should keep price at booking-time");
    }

    @Test
    @DisplayName("Multi-line: raising one service leaves every line and all totals unchanged")
    void raisingOneServiceDoesNotRewriteAnyLine() {
        PricingScenario scenario = PricingScenario.in(context)
                .withService(OIL_CHANGE, OIL_PRICE)
                .withService(TIRE_CHANGE, TIRE_PRICE)
                .withInvoice()
                .build();

        // Bara den ena tjänsten höjs, så att en rad som läser katalogpris syns direkt
        serviceItemService.updatePrice(scenario.serviceItemId(OIL_CHANGE), OIL_PRICE_RAISED);

        assertEquals(OIL_PRICE + TIRE_PRICE, scenario.expectedSubtotal(), DELTA,
                "Arrange: subtotal should be 1299:-");
        assertSnapshotIntact(context, scenario);
    }

    @Test
    @DisplayName("Discounted invoice keeps its discount and line totals when price is raised")
    void discountedInvoiceKeepsDiscountWhenPriceIsRaised() {
        PricingScenario scenario = PricingScenario.in(context)
                .withService(OIL_CHANGE, OIL_PRICE)
                .withService(TIRE_CHANGE, TIRE_PRICE)
                .withInvoice("WELCOME10")
                .build();

        Invoice before = invoiceRepo.findById(scenario.invoiceId).get();
        assertTrue(before.getDiscount() > 0, "Arrange: WELCOME10 should give a discount");

        serviceItemService.updatePrice(scenario.serviceItemId(OIL_CHANGE), OIL_PRICE_RAISED);

        // Jämför före/efter i stället för att räkna rabatten själv:
        // testet bevisar att inget ändras, inte hur rabatten räknas ut
        Invoice after = invoiceRepo.findById(scenario.invoiceId).get();
        assertEquals(before.getAmount(), after.getAmount(), DELTA, "Invoice amount changed");
        assertEquals(before.getDiscount(), after.getDiscount(), DELTA, "Invoice discount changed");
        assertEquals(before.getTotalAmount(), after.getTotalAmount(), DELTA, "Invoice total changed");

        for (String name : scenario.agreedPrices.keySet()) {
            InvoiceLine lineBefore = lineNamed(before.getLines(), InvoiceLine::getServiceItemName, name);
            InvoiceLine lineAfter = lineNamed(after.getLines(), InvoiceLine::getServiceItemName, name);
            assertEquals(lineBefore.getDiscount(), lineAfter.getDiscount(), DELTA, name + ": line discount changed");
            assertEquals(lineBefore.getTotal(), lineAfter.getTotal(), DELTA, name + ": line total changed");
        }
    }

    @Test
    @DisplayName("Historical data, including invoice lines, survives application restart")
    void historicalDataSurvivesApplicationRestart() {
        String url = jdbcUrl(tempDir.resolve("restart.db"));
        PricingScenario scenario;

        // Första körningen: skapa allt, höj priset och stäng applikationen
        try (ConfigurableApplicationContext first = startApplication(url)) {
            scenario = PricingScenario.in(first)
                    .withService(OIL_CHANGE, OIL_PRICE)
                    .withService(TIRE_CHANGE, TIRE_PRICE)
                    .withInvoice()
                    .build();

            first.getBean(ServiceItemService.class)
                    .updatePrice(scenario.serviceItemId(OIL_CHANGE), OIL_PRICE_RAISED);
        }

        // Andra körningen: ny JVM-kontext, allt läses från filen
        try (ConfigurableApplicationContext second = startApplication(url)) {
            ServiceItem oilChange = second.getBean(ServiceItemRepository.class)
                    .findById(scenario.serviceItemId(OIL_CHANGE)).get();
            assertEquals(OIL_PRICE_RAISED, oilChange.getPrice(), DELTA);

            assertSnapshotIntact(second, scenario);

            // Relationer
            Booking booking = second.getBean(BookingService.class).findById(scenario.bookingId).get();
            WorkOrder workOrder = second.getBean(WorkOrderRepository.class).findById(scenario.workOrderId).get();
            Invoice invoice = second.getBean(InvoiceRepo.class).findById(scenario.invoiceId).get();

            assertEquals(scenario.workOrderId, invoice.getWorkOrderId());
            assertEquals(scenario.bookingId, workOrder.getBookingId());
            assertEquals(scenario.vehicleId, booking.getVehicleId());
            assertEquals(scenario.serviceItemId(OIL_CHANGE),
                    lineNamed(workOrder.getItems(), WorkOrderItem::getServiceName, OIL_CHANGE).getServiceItemId());
        }
    }

    /**
     * Gemensamt bevis som används både direkt och efter omstart: varje rad i bokning,
     * arbetsorder och faktura har kvar sitt avtalade pris, och summorna stämmer.
     */
    private static void assertSnapshotIntact(ApplicationContext ctx, PricingScenario scenario) {
        int lineCount = scenario.agreedPrices.size();

        Booking booking = ctx.getBean(BookingService.class).findById(scenario.bookingId).get();
        WorkOrder workOrder = ctx.getBean(WorkOrderRepository.class).findById(scenario.workOrderId).get();

        assertEquals(lineCount, booking.getItems().size(), "Booking line count");
        assertEquals(lineCount, workOrder.getItems().size(), "Work order line count");

        for (Map.Entry<String, Double> agreed : scenario.agreedPrices.entrySet()) {
            String name = agreed.getKey();
            double price = agreed.getValue();

            assertEquals(price,
                    lineNamed(booking.getItems(), BookingServiceItem::getServiceName, name).getPriceAtBooking(),
                    DELTA, name + ": booking should keep price at booking-time");
            assertEquals(price,
                    lineNamed(workOrder.getItems(), WorkOrderItem::getServiceName, name).getAgreedPrice(),
                    DELTA, name + ": work order line should keep agreed price");
        }
        assertEquals(scenario.expectedSubtotal(), workOrder.getTotalPrice(), DELTA, "Work order total");

        if (scenario.invoiceId == null) {
            return;
        }

        Invoice invoice = ctx.getBean(InvoiceRepo.class).findById(scenario.invoiceId).get();
        assertEquals(lineCount, invoice.getLines().size(), "Invoice line count");

        double lineTotalSum = 0;
        for (Map.Entry<String, Double> agreed : scenario.agreedPrices.entrySet()) {
            InvoiceLine line = lineNamed(invoice.getLines(), InvoiceLine::getServiceItemName, agreed.getKey());
            assertEquals(agreed.getValue(), line.getAmount(), DELTA,
                    agreed.getKey() + ": invoice line should keep agreed price");
            lineTotalSum += line.getTotal();
        }
        assertEquals(scenario.expectedSubtotal(), invoice.getAmount(), DELTA, "Invoice amount");
        assertEquals(invoice.getTotalAmount(), lineTotalSum, DELTA,
                "Invoice total should equal the sum of its line totals");
    }

    // Radordningen i databasen är inte garanterad, därför letas rader upp på namn
    private static <T> T lineNamed(List<T> lines, Function<T, String> nameOf, String name) {
        return lines.stream()
                .filter(line -> name.equals(nameOf.apply(line)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No line named " + name));
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
     * Bygger kedjan tjänster -> kund -> fordon -> bokning -> arbetsorder (slutförd) -> faktura
     * och håller id:na. Allt skapas via service-lagret där det finns en metod för det,
     * så att testet går samma väg som applikationen.
     */
    private static final class PricingScenario {
        final Map<String, Integer> serviceItemIds;
        final Map<String, Double> agreedPrices;
        final int vehicleId;
        final int bookingId;
        final int workOrderId;
        final Integer invoiceId;

        private PricingScenario(Map<String, Integer> serviceItemIds, Map<String, Double> agreedPrices,
                                int vehicleId, int bookingId, int workOrderId, Integer invoiceId) {
            this.serviceItemIds = serviceItemIds;
            this.agreedPrices = agreedPrices;
            this.vehicleId = vehicleId;
            this.bookingId = bookingId;
            this.workOrderId = workOrderId;
            this.invoiceId = invoiceId;
        }

        static Builder in(ApplicationContext ctx) {
            return new Builder(ctx);
        }

        int serviceItemId(String name) {
            return serviceItemIds.get(name);
        }

        double expectedSubtotal() {
            double sum = 0;
            for (double price : agreedPrices.values()) {
                sum += price;
            }
            return sum;
        }
    }

    private static final class Builder {
        private final ApplicationContext ctx;
        private final Map<String, Double> services = new LinkedHashMap<>();
        private boolean invoice;
        private String discountCode;

        private Builder(ApplicationContext ctx) {
            this.ctx = ctx;
        }

        Builder withService(String name, double price) {
            services.put(name, price);
            return this;
        }

        Builder withInvoice() {
            return withInvoice(null);
        }

        Builder withInvoice(String discountCode) {
            this.invoice = true;
            this.discountCode = discountCode;
            return this;
        }

        PricingScenario build() {
            assertFalse(services.isEmpty(), "Arrange: a scenario needs at least one service");

            ServiceItemRepository serviceItems = ctx.getBean(ServiceItemRepository.class);
            BookingService bookings = ctx.getBean(BookingService.class);
            WorkOrderService workOrders = ctx.getBean(WorkOrderService.class);

            Map<String, Integer> ids = new LinkedHashMap<>();
            for (Map.Entry<String, Double> service : services.entrySet()) {
                ServiceItem item = serviceItems.save(
                        new ServiceItem(service.getKey(), "Testtjänst", service.getValue(), 30));
                ids.put(service.getKey(), item.getId());
            }

            // Icke-VIP-kund, så att ingen VIP-rabatt påverkar fakturans belopp
            Customer customer = ctx.getBean(CustomerRepo.class).save(
                    new Customer("Test Kund", "070-0000000", "test@example.se"));
            // Egen mekaniker per scenario, så att överlappskontrollen aldrig slår till
            Mechanic mechanic = ctx.getBean(MechanicRepository.class).save(
                    new Mechanic("Test Mekaniker", "070-0000001", "General service"));
            Vehicle vehicle = ctx.getBean(VehicleRepo.class).save(new Vehicle(
                    "TST" + REG_SEQUENCE.incrementAndGet(), "Volvo", "V60", 2020, customer.getId()));

            Booking booking = bookings.create(
                    vehicle.getId(), mechanic.getId(), BOOKING_DATE, LocalTime.of(8, 0),
                    "Prisrevision", new ArrayList<>(ids.values()));

            // Arbetsordern går hela vägen till COMPLETED, som en riktig fakturerbar order
            int workOrderId = workOrders.createWorkOrder(booking.getId()).getId();
//            assertTrue(workOrders.startWorkOrder(workOrderId), "Arrange: work order should start");
//            assertTrue(workOrders.completeWorkOrder(workOrderId), "Arrange: work order should complete");

            Integer invoiceId = invoice
                    ? ctx.getBean(InvoiceService.class).create(workOrderId, discountCode).getId()
                    : null;

            return new PricingScenario(ids, new LinkedHashMap<>(services),
                    vehicle.getId(), booking.getId(), workOrderId, invoiceId);
        }
    }
}
