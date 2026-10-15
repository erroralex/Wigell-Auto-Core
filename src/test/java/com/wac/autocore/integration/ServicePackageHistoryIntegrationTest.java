package com.wac.autocore.integration;

import com.wac.autocore.AutoCoreConfig;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.BookingServiceItem;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.ServicePackage;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.value.Money;
import com.wac.autocore.repository.CustomerRepo;
import com.wac.autocore.repository.MechanicRepository;
import com.wac.autocore.repository.ServiceItemRepository;
import com.wac.autocore.repository.VehicleRepo;
import com.wac.autocore.service.BookingService;
import com.wac.autocore.service.ServiceItemService;
import com.wac.autocore.service.ServicePackageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.wac.autocore.testsupport.MoneyAssert.assertMoney;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * <b>ServicePackageHistoryIntegrationTest</b>
 * <p>Ansvar: Bevisar kundkravet att en ändring i ett tjänstepaket bara påverkar framtida
 * bokningar. En bokning som redan skapats från paketet behåller sina rader och priser.</p>
 * <p>Körs mot en riktig SQLite-fil som Flyway bygger upp. Testmetoden är inte
 * {@code @Transactional}, så allt som läses tillbaka kommer från databasfilen.</p>
 */
@SpringBootTest(classes = AutoCoreConfig.class)
@ActiveProfiles("test")
@DirtiesContext
class ServicePackageHistoryIntegrationTest {

    private static final String WINTER_CHECK = "Winter check (test)";
    private static final String BRAKE_FLUID = "Brake fluid";

    private static final Money BRAKE_PRICE = Money.of(2495);
    private static final Money BRAKE_PRICE_RAISED = Money.of(2995);
    private static final Money DIAGNOSTICS_PRICE = Money.of(1195);
    private static final Money BRAKE_FLUID_PRICE = Money.of(495);

    private static final LocalDate DATE = LocalDate.of(2026, 11, 3);

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void sqliteFile(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:sqlite:" + tempDir.resolve("service-package-history.db").toAbsolutePath());
    }

    @Autowired private BookingService bookingService;
    @Autowired private ServicePackageService servicePackageService;
    @Autowired private ServiceItemService serviceItemService;
    @Autowired private ServiceItemRepository serviceItemRepository;
    @Autowired private CustomerRepo customerRepo;
    @Autowired private MechanicRepository mechanicRepo;
    @Autowired private VehicleRepo vehicleRepo;

    @Test
    @DisplayName("Changing a package does not change bookings already made from it")
    void changingPackageDoesNotChangeOldBooking() {

        // Arrange: egna tjänster och paket
        ServiceItem brakes = serviceItemRepository.save(
                new ServiceItem("Brake service (test)", "Test", BRAKE_PRICE, 90));
        ServiceItem diagnostics = serviceItemRepository.save(
                new ServiceItem("Diagnostics (test)", "Test", DIAGNOSTICS_PRICE, 60));
        ServicePackage winter = servicePackageService.create(
                WINTER_CHECK, "Test package", Arrays.asList(brakes.getId(), diagnostics.getId()));

        Customer customer = customerRepo.save(
                new Customer("Test Kund", "070-0000000", "test@example.se"));

        // Egen mekaniker, så att överlappskontrollen aldrig krockar med seedade bokningar
        Mechanic mechanic = mechanicRepo.save(
                new Mechanic("Test Mekaniker", "070-0000001", "General service"));
        Vehicle vehicle = vehicleRepo.save(
                new Vehicle("TST900", "Volvo", "V60", 2020, customer.getId()));

        // Act: boka från paketet
        List<Integer> packageIds = idsOf(winter);
        int bookingId = bookingService.create(
                vehicle.getId(), mechanic.getId(), DATE, LocalTime.of(8, 0),
                WINTER_CHECK, packageIds).getId();

        Map<Integer, Money> before = pricesById(bookingService.findById(bookingId).get());
        assertEquals(2, before.size(), "Arrange: booking should have the package's 2 lines");

        // Act: lägg till Brake fluid i paketet och höj priset på en tjänst
        ServiceItem brakeFluid = serviceItemRepository.save(
                new ServiceItem(BRAKE_FLUID, "Test", BRAKE_FLUID_PRICE, 30));

        List<Integer> newIds = new ArrayList<>(packageIds);
        newIds.add(brakeFluid.getId());
        servicePackageService.update(winter.getId(), WINTER_CHECK, "Test package", newIds);
        serviceItemService.updatePrice(brakes.getId(), BRAKE_PRICE_RAISED);

        // Kontroll: ändringarna har faktiskt gått igenom
        ServicePackage updated = servicePackageService.findById(winter.getId()).get();
        assertEquals(3, updated.getServices().size(),
                "Arrange: package should now contain 3 services");
        assertMoney(BRAKE_PRICE_RAISED,
                serviceItemRepository.findById(brakes.getId()).get().getPrice(),
                "Arrange: catalogue price should be raised");

        // Assert: den gamla bokningen är orörd
        Booking reloaded = bookingService.findById(bookingId).get();
        Map<Integer, Money> after = pricesById(reloaded);

        assertEquals(before.keySet(), after.keySet(), "Old booking lines changed");
        for (Map.Entry<Integer, Money> line : before.entrySet()) {
            assertMoney(line.getValue(), after.get(line.getKey()),
                    "Price changed for service " + line.getKey());
        }
        assertFalse(after.containsKey(brakeFluid.getId()),
                "Brake fluid must not be added to an existing booking");
        assertMoney(BRAKE_PRICE.add(DIAGNOSTICS_PRICE), reloaded.getTotalAgreedPrice(),
                "Booking total should still be the price at booking-time");
    }

    private static List<Integer> idsOf(ServicePackage servicePackage) {
        return servicePackage.getServices().stream()
                .map(ServiceItem::getId)
                .collect(Collectors.toList());
    }

    private static Map<Integer, Money> pricesById(Booking booking) {
        return booking.getItems().stream()
                .collect(Collectors.toMap(
                        BookingServiceItem::getServiceItemId,
                        BookingServiceItem::getPriceAtBooking));
    }
}
