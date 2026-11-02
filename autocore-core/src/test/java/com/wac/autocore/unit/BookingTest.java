package com.wac.autocore.unit;

import com.wac.autocore.exception.BookingLockedException;
import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.BookingServiceItem;
import com.wac.autocore.model.BookingStatus;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.model.value.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.wac.autocore.testsupport.MoneyAssert.assertMoney;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * <b>BookingTest</b>
 * <p>Responsibility: Proves that {@link Booking} owns its rules: the lock, at least one service, agreed prices (snapshots),
 * end time from snapshot minutes, required date and start time, that the booking does not pass midnight,
 * that the lines are read-only, that {@code copyAsNew} uses today's prices and that cancellation
 * is only allowed before a work order exists.
 * The suggested mechanic is optional and can be changed, but is not a reservation.</p>
 * <p>The services are mocks with unique ids, because real {@link ServiceItem}s get id 0 until they are saved
 * and would then count as the same service.</p>
 */
class BookingTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 2);
    private static final LocalTime EIGHT = LocalTime.of(8, 0);
    private static final String DESCRIPTION = "Winter service";

    private ServiceItem oil;
    private ServiceItem brakes;
    private ServiceItem diag;

    @BeforeEach
    void setUp() {
        oil = service(1, "Oil change", "899.50", 45);
        brakes = service(2, "Brake inspection", "1295.00", 60);
        diag = service(3, "Diagnostics", "598.50", 60);
    }

    // ----- The constructor -----

    @Test
    @DisplayName("Spec example: three services from 08:00 give 2 793,00 kr, 165 min and end 10:45")
    void constructorCalculatesTotalsAndEndTimeForSpecExample() {
        Booking booking = new Booking(1, 2, DATE, EIGHT, DESCRIPTION, Arrays.asList(oil, brakes, diag));

        assertMoney(Money.of("2793.00"), booking.getTotalAgreedPrice());
        assertEquals(165, booking.getTotalDurationMinutes());
        assertEquals(LocalTime.of(10, 45), booking.getEndTime());
        assertEquals(BookingStatus.BOOKED, booking.getStatus());
        assertEquals(Arrays.asList(1, 2, 3), serviceIds(booking));
    }

    @Test
    @DisplayName("The constructor requires a service, date and start time and rejects a booking that ends at midnight (22:00 + 120 min)")
    void constructorEnforcesBookingRules() {
        assertMessageKey("error.serviceSelect",
                () -> new Booking(1, 2, DATE, EIGHT, DESCRIPTION, null));
        assertMessageKey("error.serviceSelect",
                () -> new Booking(1, 2, DATE, EIGHT, DESCRIPTION, Collections.<ServiceItem>emptyList()));
        assertMessageKey("error.dateSelect",
                () -> new Booking(1, 2, null, EIGHT, DESCRIPTION, Arrays.asList(oil)));
        assertMessageKey("error.startTimeSelect",
                () -> new Booking(1, 2, DATE, null, DESCRIPTION, Arrays.asList(oil)));
        // The edge cases (before, exactly at and after midnight) are tested in TimeSlotTest.
        assertMessageKey("error.booking.pastMidnight",
                () -> new Booking(1, 2, DATE, LocalTime.of(22, 0), DESCRIPTION, Arrays.asList(brakes, diag)));
    }

    // ----- update -----

    @Test
    @DisplayName("update keeps the agreed price for an existing service and snapshots only new services at today's price")
    void updateKeepsExistingSnapshotsAndSnapshotsOnlyNewServices() {
        Booking booking = new Booking(1, 2, DATE, EIGHT, DESCRIPTION, Arrays.asList(oil, brakes));
        when(oil.getPrice()).thenReturn(Money.of("999.50"));
        when(diag.getPrice()).thenReturn(Money.of("650.00"));

        booking.update(1, 2, DATE, EIGHT, DESCRIPTION, Arrays.asList(oil, brakes, diag));

        assertMoney(Money.of("899.50"), item(booking, 1).getPriceAtBooking());
        assertMoney(Money.of("650.00"), item(booking, 3).getPriceAtBooking());
        assertMoney(Money.of("2844.50"), booking.getTotalAgreedPrice());
        assertEquals(LocalTime.of(10, 45), booking.getEndTime());
    }

    @Test
    @DisplayName("update removes deselected services and calculates the end time from snapshot minutes, not the catalog's")
    void updateRemovesDeselectedServicesAndUsesSnapshotDuration() {
        Booking booking = new Booking(1, 2, DATE, EIGHT, DESCRIPTION, Arrays.asList(oil, brakes));
        when(oil.getEstimatedMinutes()).thenReturn(90);

        booking.update(1, 2, DATE, EIGHT, DESCRIPTION, Arrays.asList(oil));

        assertEquals(Arrays.asList(1), serviceIds(booking));
        assertEquals(45, booking.getTotalDurationMinutes());
        assertEquals(LocalTime.of(8, 45), booking.getEndTime());
    }

    @Test
    @DisplayName("update changes vehicle, mechanic, date, start time and description")
    void updateChangesAllFields() {
        Booking booking = new Booking(1, 2, DATE, EIGHT, DESCRIPTION, Arrays.asList(oil));
        LocalDate newDate = DATE.plusDays(1);

        booking.update(3, 4, newDate, LocalTime.of(10, 0), "New description", Arrays.asList(oil));

        assertEquals(3, booking.getVehicleId());
        assertEquals(4, booking.getMechanicId());
        assertEquals(newDate, booking.getDate());
        assertEquals(LocalTime.of(10, 0), booking.getStartTime());
        assertEquals(LocalTime.of(10, 45), booking.getEndTime());
        assertEquals("New description", booking.getDescription());
    }

    @Test
    @DisplayName("update requires a service, date and start time")
    void updateRequiresServicesDateAndStartTime() {
        Booking booking = new Booking(1, 2, DATE, EIGHT, DESCRIPTION, Arrays.asList(oil));

        assertMessageKey("error.serviceSelect",
                () -> booking.update(1, 2, DATE, EIGHT, DESCRIPTION, null));
        assertMessageKey("error.serviceSelect",
                () -> booking.update(1, 2, DATE, EIGHT, DESCRIPTION, Collections.<ServiceItem>emptyList()));
        assertMessageKey("error.dateSelect",
                () -> booking.update(1, 2, null, EIGHT, DESCRIPTION, Arrays.asList(oil)));
        assertMessageKey("error.startTimeSelect",
                () -> booking.update(1, 2, DATE, null, DESCRIPTION, Arrays.asList(oil)));
    }

    @Test
    @DisplayName("A failed update leaves the booking completely unchanged")
    void failedUpdateLeavesBookingUnchanged() {
        Booking booking = new Booking(1, 2, DATE, EIGHT, DESCRIPTION, Arrays.asList(oil));

        assertMessageKey("error.booking.pastMidnight",
                () -> booking.update(3, 4, DATE.plusDays(1), LocalTime.of(23, 0), "New description",
                        Arrays.asList(brakes, diag)));

        assertEquals(1, booking.getVehicleId());
        assertEquals(2, booking.getMechanicId());
        assertEquals(DATE, booking.getDate());
        assertEquals(EIGHT, booking.getStartTime());
        assertEquals(LocalTime.of(8, 45), booking.getEndTime());
        assertEquals(DESCRIPTION, booking.getDescription());
        assertEquals(Arrays.asList(1), serviceIds(booking));
    }

    // ----- Suggested mechanic -----

    @Test
    @DisplayName("The suggested mechanic is optional and update can change and remove it")
    void suggestedMechanicIsOptionalAndCanBeChangedAndCleared() {
        Booking booking = new Booking(1, null, DATE, EIGHT, DESCRIPTION, Arrays.asList(oil));
        assertNull(booking.getMechanicId());

        booking.update(1, 4, DATE, EIGHT, DESCRIPTION, Arrays.asList(oil));
        assertEquals(4, booking.getMechanicId());

        booking.update(1, null, DATE, EIGHT, DESCRIPTION, Arrays.asList(oil));
        assertNull(booking.getMechanicId());
    }

    // ----- The lock -----

    @Test
    @DisplayName("update on a booking that is locked by an ongoing work order or cancelled throws BookingLockedException and changes nothing")
    void updateOnLockedBookingThrows() {
        Booking inProgress = new Booking(1, 2, DATE, EIGHT, DESCRIPTION, Arrays.asList(oil));
        inProgress.followWorkOrder(WorkOrderStatus.IN_PROGRESS);
        Booking cancelled = new Booking(1, 2, DATE, EIGHT, DESCRIPTION, Arrays.asList(oil));
        cancelled.cancel();

        for (Booking booking : Arrays.asList(inProgress, cancelled)) {
            assertThrows(BookingLockedException.class,
                    () -> booking.update(1, 2, DATE, EIGHT, DESCRIPTION, Arrays.asList(oil, brakes)),
                    "Status " + booking.getStatus());
            assertEquals(Arrays.asList(1), serviceIds(booking));
        }
    }

    @Test
    @DisplayName("A cancelled work order unlocks the booking again (semantics from #123)")
    void updateIsAllowedAgainAfterWorkOrderIsCancelled() {
        Booking booking = new Booking(1, 2, DATE, EIGHT, DESCRIPTION, Arrays.asList(oil));
        booking.followWorkOrder(WorkOrderStatus.IN_PROGRESS);
        booking.followWorkOrder(WorkOrderStatus.CANCELLED);

        booking.update(1, 2, DATE, EIGHT, DESCRIPTION, Arrays.asList(oil, brakes));

        assertEquals(Arrays.asList(1, 2), serviceIds(booking));
    }

    // ----- Cancellation -----

    @Test
    @DisplayName("cancel on a booked booking gives status CANCELLED")
    void cancelFromBookedSetsCancelled() {
        Booking booking = new Booking(1, 2, DATE, EIGHT, DESCRIPTION, Arrays.asList(oil));

        booking.cancel();

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
    }

    @Test
    @DisplayName("cancel throws BookingLockedException when the booking has a work order or is already cancelled")
    void cancelFromOtherStatusesThrows() {
        for (WorkOrderStatus workOrderStatus : Arrays.asList(
                WorkOrderStatus.DRAFT, WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.COMPLETED)) {
            Booking booking = new Booking(1, 2, DATE, EIGHT, DESCRIPTION, Arrays.asList(oil));
            booking.followWorkOrder(workOrderStatus);
            BookingStatus before = booking.getStatus();

            assertThrows(BookingLockedException.class, booking::cancel, "From " + before);
            assertEquals(before, booking.getStatus());
        }

        Booking cancelled = new Booking(1, 2, DATE, EIGHT, DESCRIPTION, Arrays.asList(oil));
        cancelled.cancel();
        assertThrows(BookingLockedException.class, cancelled::cancel);
    }

    // ----- applyWorkOrderPlan -----

    @Test
    @DisplayName("applyWorkOrderPlan moves a booking with a work order and keeps the agreed prices")
    void applyWorkOrderPlanFollowsOrderAndKeepsSnapshots() {
        Booking booking = new Booking(1, 2, DATE, EIGHT, DESCRIPTION, Arrays.asList(oil, brakes));
        booking.followWorkOrder(WorkOrderStatus.CONFIRMED);
        when(oil.getPrice()).thenReturn(Money.of("999.50"));
        LocalDate newDate = DATE.plusDays(2);

        booking.applyWorkOrderPlan(newDate, LocalTime.of(13, 0), Arrays.asList(oil, diag));

        assertEquals(newDate, booking.getDate());
        assertEquals(LocalTime.of(13, 0), booking.getStartTime());
        assertEquals(LocalTime.of(14, 45), booking.getEndTime());
        assertEquals(Arrays.asList(1, 3), serviceIds(booking));
        assertMoney(Money.of("899.50"), item(booking, 1).getPriceAtBooking());
        assertEquals(BookingStatus.WORK_ORDER_CREATED, booking.getStatus());
        assertEquals(1, booking.getVehicleId());
        assertEquals(2, booking.getMechanicId());
        assertEquals(DESCRIPTION, booking.getDescription());
    }

    @Test
    @DisplayName("applyWorkOrderPlan throws BookingLockedException when the booking does not have a plannable work order")
    void applyWorkOrderPlanRequiresWorkOrderCreated() {
        Booking booked = new Booking(1, 2, DATE, EIGHT, DESCRIPTION, Arrays.asList(oil));
        assertThrows(BookingLockedException.class,
                () -> booked.applyWorkOrderPlan(DATE, EIGHT, Arrays.asList(brakes)));

        for (WorkOrderStatus workOrderStatus : Arrays.asList(WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.COMPLETED)) {
            Booking booking = new Booking(1, 2, DATE, EIGHT, DESCRIPTION, Arrays.asList(oil));
            booking.followWorkOrder(workOrderStatus);

            assertThrows(BookingLockedException.class,
                    () -> booking.applyWorkOrderPlan(DATE, EIGHT, Arrays.asList(brakes)));
            assertEquals(Arrays.asList(1), serviceIds(booking));
        }

        Booking cancelled = new Booking(1, 2, DATE, EIGHT, DESCRIPTION, Arrays.asList(oil));
        cancelled.cancel();
        assertThrows(BookingLockedException.class,
                () -> cancelled.applyWorkOrderPlan(DATE, EIGHT, Arrays.asList(brakes)));
    }

    @Test
    @DisplayName("applyWorkOrderPlan follows the same rules as update: service, date, start time and midnight")
    void applyWorkOrderPlanUsesBookingRules() {
        Booking booking = new Booking(1, 2, DATE, EIGHT, DESCRIPTION, Arrays.asList(oil));
        booking.followWorkOrder(WorkOrderStatus.DRAFT);

        assertMessageKey("error.serviceSelect",
                () -> booking.applyWorkOrderPlan(DATE, EIGHT, Collections.<ServiceItem>emptyList()));
        assertMessageKey("error.dateSelect",
                () -> booking.applyWorkOrderPlan(null, EIGHT, Arrays.asList(oil)));
        assertMessageKey("error.startTimeSelect",
                () -> booking.applyWorkOrderPlan(DATE, null, Arrays.asList(oil)));
        assertMessageKey("error.booking.pastMidnight",
                () -> booking.applyWorkOrderPlan(DATE, LocalTime.of(22, 0), Arrays.asList(brakes, diag)));
        assertEquals(EIGHT, booking.getStartTime());
        assertEquals(Arrays.asList(1), serviceIds(booking));
    }

    // ----- Read-only lines -----

    @Test
    @DisplayName("getItems() is read-only")
    void itemsAreReadOnly() {
        Booking booking = new Booking(1, 2, DATE, EIGHT, DESCRIPTION, Arrays.asList(oil));
        List<BookingServiceItem> items = booking.getItems();

        assertThrows(UnsupportedOperationException.class, () -> items.clear());
    }

    // ----- copyAsNew -----

    @Test
    @DisplayName("copyAsNew uses today's catalog price, a new date and a new time, and leaves the original untouched")
    void copyAsNewUsesCurrentCatalogPrices() {
        Booking original = new Booking(1, 2, DATE, EIGHT, DESCRIPTION, Arrays.asList(oil, brakes));
        original.followWorkOrder(WorkOrderStatus.COMPLETED);
        ServiceItem oilToday = service(1, "Oil change", "999.50", 45);
        LocalDate newDate = DATE.plusWeeks(1);

        Booking copy = original.copyAsNew(newDate, LocalTime.of(9, 0), Arrays.asList(oilToday, brakes, diag));

        assertMoney(Money.of("999.50"), item(copy, 1).getPriceAtBooking());
        assertMoney(Money.of("2294.50"), copy.getTotalAgreedPrice());
        assertEquals(Arrays.asList(1, 2), serviceIds(copy));
        assertEquals(newDate, copy.getDate());
        assertEquals(LocalTime.of(9, 0), copy.getStartTime());
        assertEquals(LocalTime.of(10, 45), copy.getEndTime());
        assertEquals(BookingStatus.BOOKED, copy.getStatus());
        assertEquals(1, copy.getVehicleId());
        assertEquals(2, copy.getMechanicId());
        assertEquals(DESCRIPTION, copy.getDescription());

        assertMoney(Money.of("899.50"), item(original, 1).getPriceAtBooking());
        assertEquals(DATE, original.getDate());
        assertEquals(BookingStatus.COMPLETED, original.getStatus());
    }

    @Test
    @DisplayName("copyAsNew throws if a service no longer exists in the catalog")
    void copyAsNewThrowsWhenServiceIsMissingFromCatalog() {
        Booking original = new Booking(1, 2, DATE, EIGHT, DESCRIPTION, Arrays.asList(oil, brakes));

        EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
                () -> original.copyAsNew(DATE.plusDays(1), EIGHT, Arrays.asList(oil)));

        assertEquals("error.serviceNotFound", e.getMessageKey());
    }

    // ----- Helper methods -----

    private static ServiceItem service(int id, String name, String price, int minutes) {
        ServiceItem item = mock(ServiceItem.class);
        when(item.getId()).thenReturn(id);
        when(item.getName()).thenReturn(name);
        when(item.getPrice()).thenReturn(Money.of(price));
        when(item.getEstimatedMinutes()).thenReturn(minutes);
        return item;
    }

    private static List<Integer> serviceIds(Booking booking) {
        return booking.getItems().stream()
                .map(BookingServiceItem::getServiceItemId)
                .collect(Collectors.toList());
    }

    private static BookingServiceItem item(Booking booking, int serviceItemId) {
        return booking.getItems().stream()
                .filter(i -> i.getServiceItemId() == serviceItemId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Service " + serviceItemId + " is missing from the booking"));
    }

    private static void assertMessageKey(String expectedKey, Executable action) {
        ValidationException e = assertThrows(ValidationException.class, action);
        assertEquals(expectedKey, e.getMessageKey());
    }
}
