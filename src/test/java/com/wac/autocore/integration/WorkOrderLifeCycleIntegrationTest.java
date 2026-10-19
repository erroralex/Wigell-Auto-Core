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
 * <p>Ansvar: Bevisar att WorkOrderService kan köra en hel livscykel utan Spring och utan databas,
 * genom att använda in-memory-implementationer av repositories.</p>
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

        // Arrange: Skapa en mekaniker, en tjänst, ett fordon och en bokning
        Mechanic mechanic = mechanicRepo.save(new Mechanic("Test Mekaniker", "070-1234567", "General"));
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
        // 1. Skapa arbetsorder från bokning
        WorkOrderScheduleResult createResult = workOrderService.createWorkOrder(bookingId);
        int workOrderId = createResult.getWorkOrder().getId();

        WorkOrder order = workOrder(workOrderId);
        assertEquals(WorkOrderStatus.DRAFT, order.getStatus());
        assertEquals(BookingStatus.WORK_ORDER_CREATED, booking(bookingId).getStatus());

        // 2. Bekräfta order
        workOrderService.confirmWorkOrder(workOrderId);
        order = workOrder(workOrderId);
        assertEquals(WorkOrderStatus.CONFIRMED, order.getStatus());
        assertEquals(BookingStatus.WORK_ORDER_CREATED, booking(bookingId).getStatus());

        // 3. Starta order
        workOrderService.startWorkOrder(workOrderId);
        order = workOrder(workOrderId);
        assertEquals(WorkOrderStatus.IN_PROGRESS, order.getStatus());
        assertEquals(BookingStatus.IN_PROGRESS, booking(bookingId).getStatus());

        // 4. Slutför order
        workOrderService.completeWorkOrder(workOrderId);
        order = workOrder(workOrderId);
        assertEquals(WorkOrderStatus.COMPLETED, order.getStatus());
        assertEquals(BookingStatus.COMPLETED, booking(bookingId).getStatus());
    }

    // Ett återöppnat utkast äger fortfarande planen, så bokningen ska förbli låst och följa ordern.
    // Bara en avbruten order släpper bokningen (BOOKED).
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
                null,                                   // mekanikern tas bort
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