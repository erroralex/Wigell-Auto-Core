package com.wac.autocore.integration;

import com.wac.autocore.AutoCoreConfig;
import com.wac.autocore.model.*;
import com.wac.autocore.model.value.Money;
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

import static com.wac.autocore.testsupport.MoneyAssert.assertMoney;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <b>HistoricalPricingIntegrationTest</b>
 * <p>Responsibility: Proves that a change in the service catalog never rewrites historical prices
 * on bookings, work orders or invoices, neither in the invoice header nor on the invoice lines.</p>
 * <p>The tests run against a real SQLite file that Flyway builds, not against mocks.
 * The test methods are not {@code @Transactional}: each service and repository call gets
 * its own persistence context, so everything read back comes from the database file and
 * not from Hibernate's cache.</p>
 */
@SpringBootTest(classes = AutoCoreConfig.class)
@ActiveProfiles("test")
@DirtiesContext
class HistoricalPricingIntegrationTest {

    private static final String OIL_CHANGE = "Oil change";
    private static final String TIRE_CHANGE = "Tire change";
    private static final Money OIL_PRICE = Money.of(899);
    private static final Money OIL_PRICE_RAISED = Money.of(999);
    private static final Money TIRE_PRICE = Money.of(400);


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
        // Arrange: 10 Oct, an oil change costs 899:-
        PricingScenario scenario = PricingScenario.in(context)
                .withService(OIL_CHANGE, OIL_PRICE)
                .withInvoice()
                .build();

        // Act: 1 Nov, the price is raised
        serviceItemService.updatePrice(scenario.serviceItemId(OIL_CHANGE), OIL_PRICE_RAISED);

        // Assert: the catalog is updated, the history is not
        assertMoney(OIL_PRICE_RAISED,
                serviceItemRepository.findById(scenario.serviceItemId(OIL_CHANGE)).get().getPrice(),
                "Catalogue price should be updated to 999:-");
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
        assertMoney(OIL_PRICE, invoice.getAmount(),
                "Invoice should keep price at booking-time, not the new catalogue price");
        assertMoney(OIL_PRICE,
                lineNamed(invoice.getLines(), InvoiceLine::getServiceItemName, OIL_CHANGE).getAmount(),
                "Invoice line should keep price at booking-time");
    }

    @Test
    @DisplayName("Multi-line: raising one service leaves every line and all totals unchanged")
    void raisingOneServiceDoesNotRewriteAnyLine() {
        PricingScenario scenario = PricingScenario.in(context)
                .withService(OIL_CHANGE, OIL_PRICE)
                .withService(TIRE_CHANGE, TIRE_PRICE)
                .withInvoice()
                .build();

        // Only one of the services is raised, so a line that reads the catalog price shows up immediately
        serviceItemService.updatePrice(scenario.serviceItemId(OIL_CHANGE), OIL_PRICE_RAISED);

        assertMoney(OIL_PRICE.add(TIRE_PRICE), scenario.expectedSubtotal(),
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
        assertTrue(before.getDiscount().isPositive(), "Arrange: WELCOME10 should give a discount");

        serviceItemService.updatePrice(scenario.serviceItemId(OIL_CHANGE), OIL_PRICE_RAISED);

        // Compares before/after instead of calculating the discount itself:
        // the test proves that nothing changes, not how the discount is calculated
        Invoice after = invoiceRepo.findById(scenario.invoiceId).get();
        assertMoney(before.getAmount(), after.getAmount(), "Invoice amount changed");
        assertMoney(before.getDiscount(), after.getDiscount(), "Invoice discount changed");
        assertMoney(before.getTotalAmount(), after.getTotalAmount(), "Invoice total changed");

        for (String name : scenario.agreedPrices.keySet()) {
            InvoiceLine lineBefore = lineNamed(before.getLines(), InvoiceLine::getServiceItemName, name);
            InvoiceLine lineAfter = lineNamed(after.getLines(), InvoiceLine::getServiceItemName, name);
            assertMoney(lineBefore.getDiscount(), lineAfter.getDiscount(), name + ": line discount changed");
            assertMoney(lineBefore.getTotal(), lineAfter.getTotal(), name + ": line total changed");
        }
    }

    @Test
    @DisplayName("Historical data, including invoice lines, survives application restart")
    void historicalDataSurvivesApplicationRestart() {
        String url = jdbcUrl(tempDir.resolve("restart.db"));
        PricingScenario scenario;

        // First run: create everything, raise the price and close the application
        try (ConfigurableApplicationContext first = startApplication(url)) {
            scenario = PricingScenario.in(first)
                    .withService(OIL_CHANGE, OIL_PRICE)
                    .withService(TIRE_CHANGE, TIRE_PRICE)
                    .withInvoice()
                    .build();

            first.getBean(ServiceItemService.class)
                    .updatePrice(scenario.serviceItemId(OIL_CHANGE), OIL_PRICE_RAISED);
        }

        // Second run: new JVM context, everything is read from the file
        try (ConfigurableApplicationContext second = startApplication(url)) {
            ServiceItem oilChange = second.getBean(ServiceItemRepository.class)
                    .findById(scenario.serviceItemId(OIL_CHANGE)).get();
            assertMoney(OIL_PRICE_RAISED, oilChange.getPrice());

            assertSnapshotIntact(second, scenario);

            // Relations
            Booking booking = second.getBean(BookingService.class).findById(scenario.bookingId).get();
            WorkOrder workOrder = second.getBean(WorkOrderRepository.class).findById(scenario.workOrderId).get();
            Invoice invoice = second.getBean(InvoiceRepo.class).findById(scenario.invoiceId).get();

            assertEquals(scenario.workOrderId, invoice.getWorkOrderId());
            assertEquals(scenario.bookingId, workOrder.getBookingId());
            assertEquals(scenario.vehicleId, booking.getVehicleId());
            assertEquals(scenario.vehicleId, workOrder.getVehicleId());
            assertEquals(scenario.serviceItemId(OIL_CHANGE),
                    lineNamed(workOrder.getItems(), WorkOrderItem::getServiceName, OIL_CHANGE).getServiceItemId());
        }
    }

    /**
     * Shared proof used both directly and after a restart: every line in the booking,
     * work order and invoice keeps its agreed price, and the totals add up.
     */
    private static void assertSnapshotIntact(ApplicationContext ctx, PricingScenario scenario) {
        int lineCount = scenario.agreedPrices.size();

        Booking booking = ctx.getBean(BookingService.class).findById(scenario.bookingId).get();
        WorkOrder workOrder = ctx.getBean(WorkOrderRepository.class).findById(scenario.workOrderId).get();

        assertEquals(lineCount, booking.getItems().size(), "Booking line count");
        assertEquals(lineCount, workOrder.getItems().size(), "Work order line count");

        for (Map.Entry<String, Money> agreed : scenario.agreedPrices.entrySet()) {
            String name = agreed.getKey();
            Money price = agreed.getValue();

            assertMoney(price,
                    lineNamed(booking.getItems(), BookingServiceItem::getServiceName, name).getPriceAtBooking(),
                    name + ": booking should keep price at booking-time");
            assertMoney(price,
                    lineNamed(workOrder.getItems(), WorkOrderItem::getServiceName, name).getAgreedPrice(),
                    name + ": work order line should keep agreed price");
        }
        assertMoney(scenario.expectedSubtotal(), workOrder.getTotalPrice(), "Work order total");

        if (scenario.invoiceId == null) {
            return;
        }

        Invoice invoice = ctx.getBean(InvoiceRepo.class).findById(scenario.invoiceId).get();
        assertEquals(lineCount, invoice.getLines().size(), "Invoice line count");

        Money lineTotalSum = Money.ZERO;
        for (Map.Entry<String, Money> agreed : scenario.agreedPrices.entrySet()) {
            InvoiceLine line = lineNamed(invoice.getLines(), InvoiceLine::getServiceItemName, agreed.getKey());
            assertMoney(agreed.getValue(), line.getAmount(),
                    agreed.getKey() + ": invoice line should keep agreed price");
            lineTotalSum = lineTotalSum.add(line.getTotal());
        }
        assertMoney(scenario.expectedSubtotal(), invoice.getAmount(), "Invoice amount");
        assertMoney(invoice.getTotalAmount(), lineTotalSum,
                "Invoice total should equal the sum of its line totals");
    }

    // The row order in the database is not guaranteed, so rows are looked up by name
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
     * Builds the chain services -> customer -> vehicle -> booking -> work order (completed) -> invoice
     * and keeps the ids. Everything is created via the service layer where there is a method for it,
     * so the test takes the same path as the application.
     */
    private static final class PricingScenario {
        final Map<String, Integer> serviceItemIds;
        final Map<String, Money> agreedPrices;
        final int vehicleId;
        final int bookingId;
        final int workOrderId;
        final Integer invoiceId;

        private PricingScenario(Map<String, Integer> serviceItemIds, Map<String, Money> agreedPrices,
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

        Money expectedSubtotal() {
            return Money.sum(agreedPrices.values());
        }
    }

    private static final class Builder {
        private final ApplicationContext ctx;
        private final Map<String, Money> services = new LinkedHashMap<>();
        private boolean invoice;
        private String discountCode;

        private Builder(ApplicationContext ctx) {
            this.ctx = ctx;
        }

        Builder withService(String name, Money price) {
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
            for (Map.Entry<String, Money> service : services.entrySet()) {
                ServiceItem item = serviceItems.save(
                        new ServiceItem(service.getKey(), "Test service", service.getValue(), 30));
                ids.put(service.getKey(), item.getId());
            }

            // Non-VIP customer, so no VIP discount affects the invoice amount
            Customer customer = ctx.getBean(CustomerRepo.class).save(
                    new Customer("Test Customer", "070-0000000", "test@example.se"));
            // A separate mechanic per scenario, so the clash check on confirmation never triggers
            Mechanic mechanic = ctx.getBean(MechanicRepository.class).save(
                    new Mechanic("Test Mechanic", "070-0000001", "General service"));
            Vehicle vehicle = ctx.getBean(VehicleRepo.class).save(new Vehicle(
                    "TST" + REG_SEQUENCE.incrementAndGet(), "Volvo", "V60", 2020, customer.getId()));

            Booking booking = bookings.create(
                    vehicle.getId(), mechanic.getId(), BOOKING_DATE, LocalTime.of(8, 0),
                    "Price revision", new ArrayList<>(ids.values()));

            // The work order goes all the way to COMPLETED, like a real invoiceable order.
            // It is created as a draft with the booking's suggested mechanic and must be confirmed before starting.
            int workOrderId = workOrders.createWorkOrder(booking.getId()).getWorkOrder().getId();

            workOrders.confirmWorkOrder(workOrderId);
            workOrders.startWorkOrder(workOrderId);
            workOrders.completeWorkOrder(workOrderId);

            assertEquals(WorkOrderStatus.COMPLETED,
                    ctx.getBean(WorkOrderRepository.class).findById(workOrderId).get().getStatus(),
                    "Arrange: work order should be completed");

            Integer invoiceId = invoice
                    ? ctx.getBean(InvoiceService.class).create(workOrderId, discountCode).getId()
                    : null;

            return new PricingScenario(ids, new LinkedHashMap<>(services),
                    vehicle.getId(), booking.getId(), workOrderId, invoiceId);
        }
    }
}
