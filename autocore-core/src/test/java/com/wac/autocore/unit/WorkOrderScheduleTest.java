package com.wac.autocore.unit;

import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.DropInWorkOrder;
import com.wac.autocore.model.PlannedWorkOrder;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.model.value.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * <b>WorkOrderScheduleTest</b>
 * <p>Responsibility: Proves the order's time slot, the clash rule for mechanics, whether the order reserves the mechanic at a point in time
 * and the suggestion for the next available time.
 * The rules are pure functions of the orders, so they are tested without Spring and without repositories.</p>
 */
class WorkOrderScheduleTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 2);
    private static final int MECHANIC = 7;

    @Test
    @DisplayName("End time is start time plus the services' duration")
    void plannedEndTime_isStartPlusDuration() {
        WorkOrder order = order(1, MECHANIC, DAY, LocalTime.of(10, 0), 45, 90);

        assertEquals(LocalTime.of(12, 15), order.getPlannedEndTime());
    }

    @Test
    @DisplayName("No start time means no end time")
    void plannedEndTime_nullWithoutStartTime() {
        WorkOrder order = order(1, MECHANIC, DAY, null, 60);

        assertNull(order.getPlannedEndTime());
    }

    @Test
    @DisplayName("Same mechanic, same day, overlapping times collide")
    void collides_sameMechanicSameDayOverlapping() {
        WorkOrder current = order(1, MECHANIC, DAY, LocalTime.of(10, 0), 60);
        WorkOrder other = confirmed(order(2, MECHANIC, DAY, LocalTime.of(10, 30), 60));

        assertTrue(current.collidesWith(other));
    }

    // Each case is a reason why the orders do not clash; the same clash rule is used by nextFreeStartTime.
    @ParameterizedTest(name = "{0}")
    @MethodSource("noCollisionCases")
    @DisplayName("No collision")
    void noCollision(String reason, WorkOrder current, WorkOrder other) {
        assertFalse(current.collidesWith(other));
    }

    static Stream<Arguments> noCollisionCases() {
        LocalTime ten = LocalTime.of(10, 0);
        WorkOrder cancelled = confirmed(order(2, MECHANIC, DAY, ten, 60));
        cancelled.cancel();
        WorkOrder completed = confirmed(order(2, MECHANIC, DAY, ten, 60));
        completed.start();
        completed.complete();
        return Stream.of(
                Arguments.of("Another day",
                        order(1, MECHANIC, DAY, ten, 60), confirmed(order(2, MECHANIC, DAY.plusDays(1), ten, 60))),
                Arguments.of("Another mechanic",
                        order(1, MECHANIC, DAY, ten, 60), confirmed(order(2, 8, DAY, ten, 60))),
                Arguments.of("Adjacent slots (10-11 and 11-12)",
                        order(1, MECHANIC, DAY, LocalTime.of(11, 0), 60), confirmed(order(2, MECHANIC, DAY, ten, 60))),
                Arguments.of("Missing start time",
                        order(1, MECHANIC, DAY, null, 60), confirmed(order(2, MECHANIC, DAY, ten, 60))),
                Arguments.of("Missing mechanic",
                        order(1, null, DAY, ten, 60), confirmed(order(2, MECHANIC, DAY, ten, 60))),
                Arguments.of("The same order loaded again",
                        confirmed(order(1, MECHANIC, DAY, ten, 60)), confirmed(order(1, MECHANIC, DAY, ten, 60))),
                Arguments.of("A draft does not reserve the mechanic",
                        order(1, MECHANIC, DAY, ten, 60), order(2, MECHANIC, DAY, ten, 60)),
                Arguments.of("A cancelled order does not reserve the mechanic",
                        order(1, MECHANIC, DAY, ten, 60), cancelled),
                Arguments.of("A completed order does not reserve the mechanic",
                        order(1, MECHANIC, DAY, ten, 60), completed));
    }

    @Test
    @DisplayName("An order in progress still reserves the mechanic")
    void collides_otherIsInProgress() {
        WorkOrder current = order(1, MECHANIC, DAY, LocalTime.of(10, 0), 60);
        WorkOrder other = confirmed(order(2, MECHANIC, DAY, LocalTime.of(10, 0), 60));
        other.start();

        assertTrue(current.collidesWith(other));
    }

    // Midnight: the same rule and key as the booking. The draft may be saved, the confirmation blocks.
    // The edge cases (before, exactly at and after midnight) are tested in TimeSlotTest.

    @Test
    @DisplayName("A draft ending past midnight can be saved, but confirm() rejects it (23:00 + 90 min)")
    void pastMidnight_draftCanBeSavedButNotConfirmed() {
        WorkOrder order = order(1, MECHANIC, DAY, LocalTime.of(23, 0), 90);
        assertEquals(WorkOrderStatus.DRAFT, order.getStatus());

        ValidationException e = assertThrows(ValidationException.class, order::confirm);

        assertEquals("error.booking.pastMidnight", e.getMessageKey());
        assertEquals(WorkOrderStatus.DRAFT, order.getStatus(), "Status must not change");
    }

    // Planned order: time and suggested mechanic come from the booking. The order is a draft until it is confirmed.

    @Test
    @DisplayName("createFrom copies date, start time and the suggested mechanic (if any) into a draft")
    void createFrom_copiesTimeAndSuggestedMechanicIntoDraft() {
        WorkOrder order = plannedFromBooking(MECHANIC);

        assertEquals(WorkOrderStatus.DRAFT, order.getStatus());
        assertEquals(DAY, order.getPlannedDate());
        assertEquals(LocalTime.of(10, 0), order.getPlannedStartTime());
        assertEquals(LocalTime.of(11, 45), order.getPlannedEndTime());
        assertEquals(MECHANIC, order.getMechanicId());

        WorkOrder withoutSuggestion = plannedFromBooking(null);
        assertEquals(WorkOrderStatus.DRAFT, withoutSuggestion.getStatus());
        assertNull(withoutSuggestion.getMechanicId());
    }

    @Test
    @DisplayName("confirm() requires mechanic, date and start time on a planned order")
    void confirmPlanned_requiresMechanicDateAndStartTime() {
        WorkOrder withoutMechanic = plannedFromBooking(null);
        WorkOrder withoutDate = plannedFromBooking(MECHANIC);
        withoutDate.setPlannedDate(null);
        WorkOrder withoutStartTime = plannedFromBooking(MECHANIC);
        withoutStartTime.setPlannedStartTime(null);

        assertConfirmRejected("error.workOrder.missingMechanic", withoutMechanic);
        assertConfirmRejected("error.dateSelect", withoutDate);
        assertConfirmRejected("error.startTimeSelect", withoutStartTime);
    }

    @Test
    @DisplayName("confirm() accepts a planned order with mechanic, date and start time")
    void confirmPlanned_acceptsComplete() {
        WorkOrder order = plannedFromBooking(MECHANIC);

        order.confirm();

        assertEquals(WorkOrderStatus.CONFIRMED, order.getStatus());
    }

    // Next available time: same day, from the desired start, in 15-minute steps, before midnight. The same clash rule as collidesWith.

    @Test
    @DisplayName("Free at the wanted time: suggests the wanted time")
    void nextFree_isWantedTimeWhenFree() {
        WorkOrder current = order(1, MECHANIC, DAY, LocalTime.of(10, 0), 60);
        WorkOrder other = confirmed(order(2, MECHANIC, DAY, LocalTime.of(8, 0), 60));

        assertEquals(Optional.of(LocalTime.of(10, 0)), current.nextFreeStartTime(Arrays.asList(other)));
    }

    @Test
    @DisplayName("Collision: suggests the first 15-minute step after the reserved order (10:50 -> 11:00)")
    void nextFree_firstQuarterAfterCollision() {
        WorkOrder current = order(1, MECHANIC, DAY, LocalTime.of(10, 0), 60);
        WorkOrder other = confirmed(order(2, MECHANIC, DAY, LocalTime.of(10, 0), 50));

        assertEquals(Optional.of(LocalTime.of(11, 0)), current.nextFreeStartTime(Arrays.asList(other)));
    }

    @Test
    @DisplayName("Several reserved orders in a row: suggests the first gap that fits (10-11, 11-12 -> 12:00)")
    void nextFree_skipsSeveralReservedOrders() {
        WorkOrder current = order(1, MECHANIC, DAY, LocalTime.of(10, 0), 60);
        WorkOrder first = confirmed(order(2, MECHANIC, DAY, LocalTime.of(10, 0), 60));
        WorkOrder second = confirmed(order(3, MECHANIC, DAY, LocalTime.of(11, 0), 60));

        assertEquals(Optional.of(LocalTime.of(12, 0)), current.nextFreeStartTime(Arrays.asList(second, first)));
    }

    @Test
    @DisplayName("Nothing free before midnight: no suggestion")
    void nextFree_emptyWhenNothingFitsBeforeMidnight() {
        WorkOrder current = order(1, MECHANIC, DAY, LocalTime.of(22, 0), 60);
        WorkOrder other = confirmed(order(2, MECHANIC, DAY, LocalTime.of(22, 0), 60));

        assertEquals(Optional.empty(), current.nextFreeStartTime(Arrays.asList(other)));
    }

    @Test
    @DisplayName("Drafts and other mechanics' orders do not block the suggestion")
    void nextFree_ignoresDraftsAndOtherMechanics() {
        WorkOrder current = order(1, MECHANIC, DAY, LocalTime.of(10, 0), 60);
        WorkOrder draft = order(2, MECHANIC, DAY, LocalTime.of(10, 0), 60);
        WorkOrder otherMechanic = confirmed(order(3, 8, DAY, LocalTime.of(10, 0), 60));

        assertEquals(Optional.of(LocalTime.of(10, 0)),
                current.nextFreeStartTime(Arrays.asList(draft, otherMechanic)));
    }

    @Test
    @DisplayName("Without a mechanic there is nothing to suggest")
    void nextFree_emptyWithoutMechanic() {
        WorkOrder current = order(1, null, DAY, LocalTime.of(10, 0), 60);

        assertEquals(Optional.empty(), current.nextFreeStartTime(Collections.<WorkOrder>emptyList()));
    }

    // Busy at a point in time: the same conditions as the clash rule, but for a point in time instead of a slot.

    @Test
    @DisplayName("A confirmed order reserves its mechanic from start time up to (not including) end time")
    void reservesMechanicAt_fromStartUntilEnd() {
        WorkOrder order = confirmed(order(1, MECHANIC, DAY, LocalTime.of(10, 0), 60));

        assertFalse(order.reservesMechanicAt(DAY, LocalTime.of(9, 59)));
        assertTrue(order.reservesMechanicAt(DAY, LocalTime.of(10, 0)));
        assertTrue(order.reservesMechanicAt(DAY, LocalTime.of(10, 59, 59)));
        assertFalse(order.reservesMechanicAt(DAY, LocalTime.of(11, 0)));
    }

    @Test
    @DisplayName("A draft, a cancelled order or another day does not reserve the mechanic")
    void reservesMechanicAt_falseForDraftCancelledOrOtherDay() {
        WorkOrder draft = order(1, MECHANIC, DAY, LocalTime.of(10, 0), 60);
        WorkOrder cancelled = confirmed(order(2, MECHANIC, DAY, LocalTime.of(10, 0), 60));
        cancelled.cancel();
        WorkOrder confirmed = confirmed(order(3, MECHANIC, DAY, LocalTime.of(10, 0), 60));

        assertFalse(draft.reservesMechanicAt(DAY, LocalTime.of(10, 30)));
        assertFalse(cancelled.reservesMechanicAt(DAY, LocalTime.of(10, 30)));
        assertFalse(confirmed.reservesMechanicAt(DAY.plusDays(1), LocalTime.of(10, 30)));
    }

    @Test
    @DisplayName("An order in progress still reserves the mechanic at a time inside its slot")
    void reservesMechanicAt_trueInProgress() {
        WorkOrder order = confirmed(order(1, MECHANIC, DAY, LocalTime.of(10, 0), 60));
        order.start();

        assertTrue(order.reservesMechanicAt(DAY, LocalTime.of(10, 30)));
    }

    // Helper methods

    private static void assertConfirmRejected(String expectedKey, WorkOrder order) {
        ValidationException e = assertThrows(ValidationException.class, order::confirm);
        assertEquals(expectedKey, e.getMessageKey());
        assertEquals(WorkOrderStatus.DRAFT, order.getStatus(), "Status must not change");
    }

    private static WorkOrder plannedFromBooking(Integer suggestedMechanicId) {
        Booking booking = new Booking(10, suggestedMechanicId, DAY, LocalTime.of(10, 0), "Service",
                Arrays.asList(service(1, 45), service(2, 60)));
        return PlannedWorkOrder.createFrom(booking);
    }

    private static WorkOrder order(int id, Integer mechanicId, LocalDate date, LocalTime start, int... minutes) {
        DropInWorkOrder order = DropInWorkOrder.draft(10, "Noise from the front axle");
        ReflectionTestUtils.setField(order, "id", id);
        order.setMechanicId(mechanicId);
        order.setPlannedDate(date);
        order.setPlannedStartTime(start);
        for (int i = 0; i < minutes.length; i++) {
            order.addItem(service(i + 1, minutes[i]));
        }
        return order;
    }

    private static WorkOrder confirmed(WorkOrder order) {
        order.confirm();
        return order;
    }

    private static ServiceItem service(int id, int minutes) {
        ServiceItem service = mock(ServiceItem.class);
        when(service.getId()).thenReturn(id);
        when(service.getName()).thenReturn("Service " + id);
        when(service.getPrice()).thenReturn(Money.of(500));
        when(service.getEstimatedMinutes()).thenReturn(minutes);
        return service;
    }
}
