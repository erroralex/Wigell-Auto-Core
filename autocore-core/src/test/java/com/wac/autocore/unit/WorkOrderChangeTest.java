package com.wac.autocore.unit;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.PlannedWorkOrder;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderChange;
import com.wac.autocore.model.WorkOrderChangeType;
import com.wac.autocore.model.value.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.wac.autocore.testsupport.MoneyAssert.assertMoney;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * <b>WorkOrderChangeTest</b>
 * <p>Responsibility: Proves that {@link WorkOrderChange#between(WorkOrder, WorkOrder)} finds what differs between a cancelled
 * order and the new order created from the booking on reopening: services, agreed price, duration,
 * mechanic and vehicle.</p>
 * <p>The orders are built from real bookings, just as on reopening. The services are mocks with unique ids,
 * because real {@link ServiceItem}s get id 0 until they are saved.</p>
 */
class WorkOrderChangeTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 2);
    private static final LocalTime EIGHT = LocalTime.of(8, 0);
    private static final String DESCRIPTION = "Winter service";

    private ServiceItem oil;
    private ServiceItem brakes;

    @BeforeEach
    void setUp() {
        oil = service(1, "Oil change", "899.00", 45);
        brakes = service(2, "Brake inspection", "1295.00", 60);
    }

    @Test
    void noChangesWhenOrdersAreEqual() {
        WorkOrder previous = orderFor(1, 2, oil, brakes);
        WorkOrder current = orderFor(1, 2, oil, brakes);

        assertTrue(WorkOrderChange.between(previous, current).isEmpty());
    }

    @Test
    void reportsAddedService() {
        WorkOrder previous = orderFor(1, 2, oil);
        WorkOrder current = orderFor(1, 2, oil, brakes);

        List<WorkOrderChange> changes = WorkOrderChange.between(previous, current);

        assertEquals(1, changes.size());
        WorkOrderChange change = changes.get(0);
        assertEquals(WorkOrderChangeType.SERVICE_ADDED, change.getType());
        assertEquals("Brake inspection", change.getServiceName());
        assertNull(change.getPreviousItem());
        assertEquals(2, change.getCurrentItem().getServiceItemId());
    }

    @Test
    void reportsRemovedService() {
        WorkOrder previous = orderFor(1, 2, oil, brakes);
        WorkOrder current = orderFor(1, 2, oil);

        List<WorkOrderChange> changes = WorkOrderChange.between(previous, current);

        assertEquals(1, changes.size());
        WorkOrderChange change = changes.get(0);
        assertEquals(WorkOrderChangeType.SERVICE_REMOVED, change.getType());
        assertEquals("Brake inspection", change.getServiceName());
        assertEquals(2, change.getPreviousItem().getServiceItemId());
        assertNull(change.getCurrentItem());
    }

    @Test
    void reportsChangedAgreedPrice() {
        WorkOrder previous = orderFor(1, 2, oil);
        ServiceItem dearerOil = service(1, "Oil change", "999.00", 45);
        WorkOrder current = orderFor(1, 2, dearerOil);

        List<WorkOrderChange> changes = WorkOrderChange.between(previous, current);

        assertEquals(1, changes.size());
        WorkOrderChange change = changes.get(0);
        assertEquals(WorkOrderChangeType.PRICE_CHANGED, change.getType());
        assertEquals("Oil change", change.getServiceName());
        assertMoney(Money.of(899), change.getPreviousItem().getAgreedPrice());
        assertMoney(Money.of(999), change.getCurrentItem().getAgreedPrice());
    }

    @Test
    void reportsChangedDuration() {
        WorkOrder previous = orderFor(1, 2, oil);
        ServiceItem slowerOil = service(1, "Oil change", "899.00", 60);
        WorkOrder current = orderFor(1, 2, slowerOil);

        List<WorkOrderChange> changes = WorkOrderChange.between(previous, current);

        assertEquals(1, changes.size());
        WorkOrderChange change = changes.get(0);
        assertEquals(WorkOrderChangeType.DURATION_CHANGED, change.getType());
        assertEquals(45, change.getPreviousItem().getDurationMinutes());
        assertEquals(60, change.getCurrentItem().getDurationMinutes());
    }

    @Test
    void reportsChangedMechanic() {
        WorkOrder previous = orderFor(1, 2, oil);
        WorkOrder current = orderFor(1, 7, oil);

        List<WorkOrderChange> changes = WorkOrderChange.between(previous, current);

        assertEquals(1, changes.size());
        WorkOrderChange change = changes.get(0);
        assertEquals(WorkOrderChangeType.MECHANIC_CHANGED, change.getType());
        assertEquals(Integer.valueOf(2), change.getPreviousId());
        assertEquals(Integer.valueOf(7), change.getCurrentId());
        assertNull(change.getServiceName());
    }

    @Test
    void reportsChangedVehicle() {
        WorkOrder previous = orderFor(1, 2, oil);
        WorkOrder current = orderFor(9, 2, oil);

        List<WorkOrderChange> changes = WorkOrderChange.between(previous, current);

        assertEquals(1, changes.size());
        WorkOrderChange change = changes.get(0);
        assertEquals(WorkOrderChangeType.VEHICLE_CHANGED, change.getType());
        assertEquals(Integer.valueOf(1), change.getPreviousId());
        assertEquals(Integer.valueOf(9), change.getCurrentId());
    }

    @Test
    void reportsChangedDateAndStartTime() {
        WorkOrder previous = orderFor(1, 2, oil);
        WorkOrder current = PlannedWorkOrder.createFrom(
                new Booking(1, 2, DATE.plusDays(1), LocalTime.of(13, 0), DESCRIPTION, Arrays.asList(oil)));

        List<WorkOrderChange> changes = WorkOrderChange.between(previous, current);

        assertEquals(Arrays.asList(WorkOrderChangeType.DATE_CHANGED, WorkOrderChangeType.START_TIME_CHANGED),
                types(changes));
        assertEquals(DATE, changes.get(0).getPreviousDate());
        assertEquals(DATE.plusDays(1), changes.get(0).getCurrentDate());
        assertEquals(EIGHT, changes.get(1).getPreviousStartTime());
        assertEquals(LocalTime.of(13, 0), changes.get(1).getCurrentStartTime());
        assertNull(changes.get(0).getServiceName());
    }

    @Test
    void reportsChangesInFixedOrder() {
        WorkOrder previous = orderFor(1, 2, oil);
        ServiceItem dearerOil = service(1, "Oil change", "999.00", 45);
        WorkOrder current = orderFor(9, 7, dearerOil, brakes);

        List<WorkOrderChange> changes = WorkOrderChange.between(previous, current);

        assertEquals(Arrays.asList(
                        WorkOrderChangeType.VEHICLE_CHANGED,
                        WorkOrderChangeType.MECHANIC_CHANGED,
                        WorkOrderChangeType.PRICE_CHANGED,
                        WorkOrderChangeType.SERVICE_ADDED),
                types(changes));
    }

    @Test
    void returnedListIsReadOnly() {
        List<WorkOrderChange> changes = WorkOrderChange.between(orderFor(1, 2, oil), orderFor(1, 7, oil));

        assertThrows(UnsupportedOperationException.class, () -> changes.clear());
    }

    private static WorkOrder orderFor(int vehicleId, int mechanicId, ServiceItem... services) {
        Booking booking = new Booking(vehicleId, mechanicId, DATE, EIGHT, DESCRIPTION, Arrays.asList(services));
        return PlannedWorkOrder.createFrom(booking);
    }

    private static List<WorkOrderChangeType> types(List<WorkOrderChange> changes) {
        return changes.stream().map(WorkOrderChange::getType).collect(Collectors.toList());
    }

    private static ServiceItem service(int id, String name, String price, int minutes) {
        ServiceItem service = mock(ServiceItem.class);
        when(service.getId()).thenReturn(id);
        when(service.getName()).thenReturn(name);
        when(service.getPrice()).thenReturn(Money.of(price));
        when(service.getEstimatedMinutes()).thenReturn(minutes);
        return service;
    }
}
