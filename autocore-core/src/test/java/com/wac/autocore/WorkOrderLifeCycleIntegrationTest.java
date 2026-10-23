package com.wac.autocore.integration;

import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.BookingStatus;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.model.value.Money;
import com.wac.autocore.service.BookingService;
import com.wac.autocore.service.WorkOrderScheduleResult;
import com.wac.autocore.service.WorkOrderService;
import com.wac.autocore.testsupport.InMemoryBookingRepository;
import com.wac.autocore.testsupport.InMemoryMechanicRepository;
import com.wac.autocore.testsupport.InMemoryServiceItemRepository;
import com.wac.autocore.testsupport.InMemoryVehicleRepo;
import com.wac.autocore.testsupport.InMemoryWorkOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <b>WorkOrderLifecycleInMemoryTest</b>
 * <p>Responsibility: Proves that WorkOrderService can run a whole lifecycle without Spring and without a database,
 * by using in-memory implementations of the repositories.</p>
 */
class WorkOrderLifeCycleIntegrationTest {

    private InMemoryWorkOrderRepository workOrderRepo;
    private InMemoryBookingRepository bookingRepo;
    private InMemoryMechanicRepository mechanicRepo;
    private InMemoryVehicleRepo vehicleRepo;
    private InMemoryServiceItemRepository serviceItemRepo;

    private WorkOrderService workOrderService;
    private BookingService bookingService;

    private int bookingId;
    private int mechanicId;

    @BeforeEach
    void setUp() {
        workOrderRepo = new InMemoryWorkOrderRepository();
        bookingRepo = new InMemoryBookingRepository(workOrderRepo);
        mechanicRepo = new InMemoryMechanicRepository();
        vehicleRepo = new InMemoryVehicleRepo();
        serviceItemRepo = new InMemoryServiceItemRepository();

        bookingService = new BookingService(bookingRepo, serviceItemRepo, mechanicRepo, vehicleRepo);
        workOrderService = new WorkOrderService(workOrderRepo, bookingRepo, mechanicRepo, vehicleRepo, serviceItemRepo, bookingService);

        // Arrange: Create a mechanic, a service, a vehicle and a booking
        Mechanic mechanic = mechanicRepo.save(new Mechanic("Test Mechanic", "070-1234567", "General"));
        mechanicId = mechanic.getId();

        ServiceItem service = serviceItemRepo.save(
                new ServiceItem("Test Service", "Desc", Money.of("100.00"), 60)
        );

        Vehicle vehicle = vehicleRepo.save(new Vehicle("ABC123", "Volvo", "V60", 2020, 1));

        Booking booking = bookingRepo.save(new Booking(
                vehicle.getId(),
                mechanicId,
                LocalDate.now(),
                LocalTime.of(10, 0),
                "Test",
                Collections.singletonList(service)
        ));
        bookingId = booking.getId();
    }

    @Test
    @DisplayName("Full work order lifecycle runs without Spring and without a database")
    void fullLifecycleRunsInMemory() {
        // 1. Create work order from booking
        WorkOrderScheduleResult createResult = workOrderService.createWorkOrder(bookingId);
        int workOrderId = createResult.getWorkOrder().getId();

        WorkOrder order = workOrder(workOrderId);
        assertEquals(WorkOrderStatus.DRAFT, order.getStatus());
        assertEquals(BookingStatus.WORK_ORDER_CREATED, booking(bookingId).getStatus());

        // 2. Confirm order
        workOrderService.confirmWorkOrder(workOrderId);
        order = workOrder(workOrderId);
        assertEquals(WorkOrderStatus.CONFIRMED, order.getStatus());
        assertEquals(BookingStatus.WORK_ORDER_CREATED, booking(bookingId).getStatus());

        // 3. Start order
        workOrderService.startWorkOrder(workOrderId);
        order = workOrder(workOrderId);
        assertEquals(WorkOrderStatus.IN_PROGRESS, order.getStatus());
        assertEquals(BookingStatus.IN_PROGRESS, booking(bookingId).getStatus());

        // 4. Complete order
        workOrderService.completeWorkOrder(workOrderId);
        order = workOrder(workOrderId);
        assertEquals(WorkOrderStatus.COMPLETED, order.getStatus());
        assertEquals(BookingStatus.COMPLETED, booking(bookingId).getStatus());
    }

    // A reopened draft still owns the plan, so the booking should remain locked and follow the order.
    // Only a cancelled order releases the booking (BOOKED).
    @Test
    @DisplayName("Reopening a confirmed work order makes it a draft and keeps its booking locked")
    void reopenConfirmedOrder_keepsBookingLocked() {
        int workOrderId = workOrderService.createWorkOrder(bookingId).getWorkOrder().getId();
        workOrderService.confirmWorkOrder(workOrderId);

        workOrderService.reopenWorkOrder(workOrderId);

        WorkOrder order = workOrder(workOrderId);
        Booking booking = booking(bookingId);
        assertEquals(WorkOrderStatus.DRAFT, order.getStatus());
        assertTrue(order.getStatus().isEditable(), "The reopened draft should be editable");
        assertEquals(BookingStatus.WORK_ORDER_CREATED, booking.getStatus());
        assertFalse(booking.getStatus().isEditable(), "The booking should stay locked while the order owns the plan");
    }
      
    @Test
    @DisplayName("A confirmed work order cannot be edited to lose its mechanic")
    void confirmedOrder_cannotLoseMechanic() {
        int workOrderId = workOrderService.createWorkOrder(bookingId).getWorkOrder().getId();
        workOrderService.confirmWorkOrder(workOrderId);
        WorkOrder order = workOrder(workOrderId);
        int serviceItemId = order.getItems().get(0).getServiceItemId();

        assertThrows(ValidationException.class, () -> workOrderService.updateWorkOrder(
                workOrderId,
                Collections.singletonList(serviceItemId),
                null,                                   // the mechanic is removed
                order.getPlannedDate(),
                order.getPlannedStartTime(),
                null, null,
                Collections.<Integer, Boolean>emptyMap()));
    }

    private WorkOrder workOrder(int id) {
        return workOrderRepo.findById(id)
                .orElseThrow(() -> new AssertionError("Work order " + id + " not found"));
    }

    private Booking booking(int id) {
        return bookingRepo.findById(id)
                .orElseThrow(() -> new AssertionError("Booking " + id + " not found"));
    }
}