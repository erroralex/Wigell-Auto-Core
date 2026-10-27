package com.wac.autocore.security;

import com.wac.autocore.AutoCoreConfig;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.repository.CustomerRepo;
import com.wac.autocore.repository.VehicleRepo;
import com.wac.autocore.service.CustomerService;
import com.wac.autocore.service.VehicleService;
import com.wac.autocore.testsupport.WithAdmin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <b>CustomerVehicleSecurityTest</b>
 * <p>Responsibility: Proves that customers and vehicles can be changed by workshop staff, but only deleted by admins,
 * and that a read-only user can only read.</p>
 * <p>It runs through the Spring proxy against its own SQLite file. Denied calls use valid arguments, so the rejection
 * comes from the security rule and not from validation. Test data is written with repositories, which have no rules.</p>
 */
@SpringBootTest(classes = AutoCoreConfig.class)
@ActiveProfiles("test")
@DirtiesContext
class CustomerVehicleSecurityTest {

    private static final AtomicInteger REG_SEQUENCE = new AtomicInteger(700);

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void sqliteFile(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:sqlite:" + tempDir.resolve("customer-vehicle-security.db").toAbsolutePath());
    }

    @Autowired private CustomerService customers;
    @Autowired private VehicleService vehicles;
    @Autowired private CustomerRepo customerRepo;
    @Autowired private VehicleRepo vehicleRepo;

    @Test
    @WithMockUser(roles = "READ_ONLY")
    @DisplayName("READ_ONLY can read customers and vehicles")
    void readOnlyCanRead() {
        assertDoesNotThrow(() -> customers.findAll());
        assertDoesNotThrow(() -> customers.findById(1));
        assertDoesNotThrow(() -> customers.existsById(1));
        assertDoesNotThrow(() -> vehicles.findAll());
        assertDoesNotThrow(() -> vehicles.findById(1));
        assertDoesNotThrow(() -> vehicles.findByCustomerId(1));
        assertDoesNotThrow(() -> vehicles.existsById(1));
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    @DisplayName("READ_ONLY cannot create, update or delete customers and vehicles")
    void readOnlyCannotChangeAnything() {
        Customer customer = seedCustomer();
        Vehicle vehicle = seedVehicle(customer.getId());

        assertDenied(() -> customers.create("New Customer", "070-1111111", "new@example.se"));
        assertDenied(() -> customers.update(customer));
        assertDenied(() -> customers.deleteById(customer.getId()));
        assertDenied(() -> vehicles.create(nextRegNr(), "Volvo", "V60", 2020, customer.getId()));
        assertDenied(() -> vehicles.update(vehicle.getId(), vehicle.getRegistrationNumber(), "Volvo", "V60", 2020,
                customer.getId()));
        assertDenied(() -> vehicles.deleteById(vehicle.getId()));

        assertTrue(customerRepo.existsById(customer.getId()));
        assertTrue(vehicleRepo.existsById(vehicle.getId()));
    }

    @Test
    @WithMockUser(roles = "WORKSHOP")
    @DisplayName("WORKSHOP can create and update customers and vehicles")
    void workshopCanCreateAndUpdate() {
        Customer customer = assertDoesNotThrow(
                () -> customers.create("Workshop Customer", "070-2222222", "workshop@example.se"));
        assertDoesNotThrow(() -> customers.update(customer));

        Vehicle vehicle = assertDoesNotThrow(
                () -> vehicles.create(nextRegNr(), "Volvo", "V60", 2020, customer.getId()));
        assertDoesNotThrow(() -> vehicles.update(vehicle.getId(), vehicle.getRegistrationNumber(), "Volvo", "V90",
                2021, customer.getId()));
    }

    @Test
    @WithMockUser(roles = "WORKSHOP")
    @DisplayName("WORKSHOP is denied deleteById on customers and vehicles, and the records remain")
    void workshopCannotDelete() {
        Customer customer = seedCustomer();
        Vehicle vehicle = seedVehicle(customer.getId());

        assertDenied(() -> vehicles.deleteById(vehicle.getId()));
        assertDenied(() -> customers.deleteById(customer.getId()));

        assertTrue(vehicleRepo.existsById(vehicle.getId()));
        assertTrue(customerRepo.existsById(customer.getId()));
    }

    @Test
    @WithAdmin
    @DisplayName("ADMIN can delete vehicles and customers")
    void adminCanDelete() {
        Customer customer = seedCustomer();
        Vehicle vehicle = seedVehicle(customer.getId());

        vehicles.deleteById(vehicle.getId());
        customers.deleteById(customer.getId());

        assertFalse(vehicleRepo.existsById(vehicle.getId()));
        assertFalse(customerRepo.existsById(customer.getId()));
    }

    private static void assertDenied(Executable call) {
        assertThrows(AccessDeniedException.class, call);
    }

    private static String nextRegNr() {
        return "CUS" + REG_SEQUENCE.incrementAndGet();
    }

    private Customer seedCustomer() {
        return customerRepo.save(new Customer("Test Customer", "070-0000000", "test@example.se"));
    }

    private Vehicle seedVehicle(int customerId) {
        return vehicleRepo.save(new Vehicle(nextRegNr(), "Volvo", "V60", 2020, customerId));
    }
}
