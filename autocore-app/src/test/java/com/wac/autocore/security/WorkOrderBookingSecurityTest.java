package com.wac.autocore.security;

import com.wac.autocore.AutoCoreConfig;
import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.model.Customer;
import com.wac.autocore.model.DropInWorkOrder;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.model.value.Money;
import com.wac.autocore.repository.CustomerRepo;
import com.wac.autocore.repository.MechanicRepository;
import com.wac.autocore.repository.ServiceItemRepository;
import com.wac.autocore.repository.VehicleRepo;
import com.wac.autocore.repository.WorkOrderRepository;
import com.wac.autocore.service.BookingService;
import com.wac.autocore.service.WorkOrderService;
import com.wac.autocore.testsupport.WithAdmin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * <b>WorkOrderBookingSecurityTest</b>
 * <p>Responsibility: Proves that work orders and bookings can only be changed by roles that work with them, and that a
 * read-only user can list work orders but cannot create, change or move one through its lifecycle.</p>
 * <p>It runs through the Spring proxy against a real SQLite file. Denied calls use valid arguments, so the
 * rejection comes from the security rule and not from validation. Test data is written with repositories, which have
 * no rules, so the seeding does not depend on the role under test.</p>
 */
@SpringBootTest(classes = AutoCoreConfig.class)
@ActiveProfiles("test")
@DirtiesContext
class WorkOrderBookingSecurityTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 10);
    private static final LocalTime TIME = LocalTime.of(8, 0);
    private static final AtomicInteger REG_SEQUENCE = new AtomicInteger(500);

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void sqliteFile(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:sqlite:" + tempDir.resolve("workorder-booking-security.db").toAbsolutePath());
    }

    @Autowired private WorkOrderService workOrders;
    @Autowired private BookingService bookings;
    @Autowired private WorkOrderRepository workOrderRepository;
    @Autowired private CustomerRepo customerRepo;
    @Autowired private VehicleRepo vehicleRepo;
    @Autowired private MechanicRepository mechanicRepository;
    @Autowired private ServiceItemRepository serviceItemRepository;

    @Test
    @WithMockUser(roles = "READ_ONLY")
    @DisplayName("READ_ONLY can list work orders and bookings")
    void readOnlyCanRead() {
        assertDoesNotThrow(() -> workOrders.findAll());
        assertDoesNotThrow(() -> workOrders.findById(1));
        assertDoesNotThrow(() -> workOrders.findByMechanicId(1));
        assertDoesNotThrow(() -> workOrders.findCompletedNotInvoiced());
        assertDoesNotThrow(() -> workOrders.findCompletedWorkOrders());
        assertDoesNotThrow(() -> workOrders.findBookableBookings());
        assertDoesNotThrow(() -> bookings.listAll());
        assertDoesNotThrow(() -> bookings.listActive());
        assertDoesNotThrow(() -> bookings.findById(1));
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    @DisplayName("READ_ONLY cannot confirm, start, complete, cancel or reopen a work order, and it stays unchanged")
    void readOnlyCannotMoveAWorkOrderThroughItsLifecycle() {
        int workOrderId = seedDraftWorkOrder();

        assertDenied(() -> workOrders.confirmWorkOrder(workOrderId));
        assertDenied(() -> workOrders.startWorkOrder(workOrderId));
        assertDenied(() -> workOrders.completeWorkOrder(workOrderId));
        assertDenied(() -> workOrders.cancelWorkOrder(workOrderId));
        assertDenied(() -> workOrders.reopenWorkOrder(workOrderId));

        assertEquals(WorkOrderStatus.DRAFT, workOrderRepository.findById(workOrderId).get().getStatus());
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    @DisplayName("READ_ONLY cannot create or update a work order")
    void readOnlyCannotCreateOrUpdateWorkOrders() {
        assertDenied(() -> workOrders.createWorkOrder(1));
        assertDenied(() -> workOrders.createWarranty(1, "Problem"));
        assertDenied(() -> workOrders.createDropIn(1, "Problem", Collections.emptyList()));
        assertDenied(() -> workOrders.updateWorkOrder(1, Collections.emptyList(), null, null, null, null, null, null));
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    @DisplayName("READ_ONLY cannot create, update or cancel a booking")
    void readOnlyCannotChangeBookings() {
        assertDenied(() -> bookings.create(1, 1, DATE, TIME, "Description", Collections.emptyList()));
        assertDenied(() -> bookings.createFromPrevious(1, DATE, TIME, null, 1, null, null));
        assertDenied(() -> bookings.update(1, 1, 1, DATE, TIME, "Description", Collections.emptyList()));
        assertDenied(() -> bookings.cancel(1));
    }

    @Test
    @WithMockUser(roles = "READ_ONLY")
    @DisplayName("READ_ONLY cannot call the methods that make a booking follow its work order")
    void readOnlyCannotCallTheFollowMethodsDirectly() {
        assertDenied(() -> bookings.followWorkOrder(1, DATE, TIME, Collections.emptyList()));
        assertDenied(() -> bookings.followWorkOrderStatus(1, WorkOrderStatus.CONFIRMED));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("An anonymous user cannot even read")
    void anonymousIsDeniedEverything() {
        assertDenied(() -> workOrders.findAll());
        assertDenied(() -> bookings.listAll());
        assertDenied(() -> workOrders.completeWorkOrder(1));
        assertDenied(() -> bookings.cancel(1));
    }

    @Test
    @WithMockUser(roles = "WORKSHOP")
    @DisplayName("WORKSHOP can run the whole lifecycle, including the internal calls to BookingService")
    void workshopCanRunTheWholeLifecycle() {
        int serviceItemId = seedServiceItem();
        int vehicleId = seedVehicle();
        int mechanicId = seedMechanic();

        int bookingId = bookings.create(vehicleId, mechanicId, DATE, TIME, "Service",
                Collections.singletonList(serviceItemId)).getId();
        int workOrderId = workOrders.createWorkOrder(bookingId).getWorkOrder().getId();

        // Each of these calls bookingService.followWorkOrderStatus through the proxy, which needs WORK_ORDER_WRITE.
        workOrders.confirmWorkOrder(workOrderId);
        workOrders.startWorkOrder(workOrderId);
        workOrders.completeWorkOrder(workOrderId);

        assertEquals(WorkOrderStatus.COMPLETED, workOrderRepository.findById(workOrderId).get().getStatus());
    }

    @Test
    @WithAdmin
    @DisplayName("ADMIN passes the security check")
    void adminPassesTheSecurityCheck() {
        // Id 999 does not exist, so the error must come from the business logic and not from security.
        assertThrows(EntityNotFoundException.class, () -> workOrders.completeWorkOrder(999));
        assertThrows(EntityNotFoundException.class, () -> bookings.cancel(999));
    }

    private static void assertDenied(Executable call) {
        assertThrows(AccessDeniedException.class, call);
    }

    private int seedDraftWorkOrder() {
        return workOrderRepository.save(DropInWorkOrder.draft(seedVehicle(), "Problem")).getId();
    }

    private int seedServiceItem() {
        return serviceItemRepository.save(new ServiceItem("Oil change", "Test service", Money.of(899), 30)).getId();
    }

    private int seedMechanic() {
        return mechanicRepository.save(new Mechanic("Test Mechanic", "070-0000001", "General service")).getId();
    }

    private int seedVehicle() {
        Customer customer = customerRepo.save(new Customer("Test Customer", "070-0000000", "test@example.se"));
        return vehicleRepo.save(new Vehicle("SEC" + REG_SEQUENCE.incrementAndGet(), "Volvo", "V60", 2020,
                customer.getId())).getId();
    }
}
