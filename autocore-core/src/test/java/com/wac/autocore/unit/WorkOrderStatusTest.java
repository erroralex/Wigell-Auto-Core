package com.wac.autocore.unit;

import com.wac.autocore.exception.IllegalStatusTransitionException;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.model.WorkOrderType;
import com.wac.autocore.model.value.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static com.wac.autocore.model.WorkOrderStatus.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * <b>WorkOrderStatusTest</b>
 * <p>Responsibility: Proves that the lifecycle rules are correct. The expected table is
 * written by hand from the requirement, so an unintentionally changed rule in the enum turns the test red.</p>
 */
class WorkOrderStatusTest {

    private static final Map<WorkOrderStatus, Set<WorkOrderStatus>> EXPECTED_ALLOWED =
            new EnumMap<>(WorkOrderStatus.class);

    static {
        EXPECTED_ALLOWED.put(DRAFT, EnumSet.of(CONFIRMED, CANCELLED));
        EXPECTED_ALLOWED.put(CONFIRMED, EnumSet.of(IN_PROGRESS, CANCELLED, DRAFT));
        EXPECTED_ALLOWED.put(IN_PROGRESS, EnumSet.of(COMPLETED, CANCELLED));
        EXPECTED_ALLOWED.put(CANCELLED, EnumSet.of(DRAFT));
        EXPECTED_ALLOWED.put(COMPLETED, EnumSet.noneOf(WorkOrderStatus.class));
    }

    static Stream<Arguments> allPairs() {
        List<Arguments> pairs = new ArrayList<>();
        for (WorkOrderStatus from : WorkOrderStatus.values()) {
            for (WorkOrderStatus to : WorkOrderStatus.values()) {
                pairs.add(Arguments.of(from, to, EXPECTED_ALLOWED.get(from).contains(to)));
            }
        }
        return pairs.stream();
    }

    @ParameterizedTest(name = "{0} -> {1} allowed={2}")
    @MethodSource("allPairs")
    void canChangeTo_matchesLifecycle(WorkOrderStatus from, WorkOrderStatus to, boolean expected) {
        assertEquals(expected, from.canChangeTo(to), from + " -> " + to);
    }

    @Test
    @DisplayName("Expected table covers every status")
    void expectedTableCoversAllStatuses() {
        assertEquals(EnumSet.allOf(WorkOrderStatus.class), EXPECTED_ALLOWED.keySet(),
                "A status was added or removed, update EXPECTED_ALLOWED");
    }

    @ParameterizedTest(name = "{0} is editable")
    @EnumSource(value = WorkOrderStatus.class, names = {"DRAFT", "CONFIRMED"})
    void isEditable_trueBeforeWorkStarts(WorkOrderStatus status) {
        assertTrue(status.isEditable(), status + " should be editable");
    }

    @ParameterizedTest(name = "{0} is locked")
    @EnumSource(value = WorkOrderStatus.class, names = {"DRAFT", "CONFIRMED"},
            mode = EnumSource.Mode.EXCLUDE)
    void isEditable_falseOnceWorkHasStarted(WorkOrderStatus status) {
        assertFalse(status.isEditable(), status + " should be locked");
    }

    @ParameterizedTest(name = "{0} reserves the mechanic")
    @EnumSource(value = WorkOrderStatus.class, names = {"CONFIRMED", "IN_PROGRESS"})
    void reservesMechanic_trueOnceConfirmedAndUntilDone(WorkOrderStatus status) {
        assertTrue(status.reservesMechanic(), status + " should reserve the mechanic");
    }

    @ParameterizedTest(name = "{0} does not reserve the mechanic")
    @EnumSource(value = WorkOrderStatus.class, names = {"CONFIRMED", "IN_PROGRESS"},
            mode = EnumSource.Mode.EXCLUDE)
    void reservesMechanic_falseForDraftAndFinished(WorkOrderStatus status) {
        assertFalse(status.reservesMechanic(), status + " should not reserve the mechanic");
    }

    @Nested
    @DisplayName("Entity methods throw on forbidden transitions")
    class EntityMethods {

        @Test
        @DisplayName("confirm() from IN_PROGRESS throws")
        void confirm_fromInProgress_throws() {
            TestWorkOrder wo = workOrderIn(IN_PROGRESS);

            IllegalStatusTransitionException ex =
                    assertThrows(IllegalStatusTransitionException.class, wo::confirm);

            assertArrayEquals(new Object[]{IN_PROGRESS, CONFIRMED}, ex.getMessageArgs());
            assertEquals(IN_PROGRESS, wo.getStatus(), "Status must not change on a forbidden transition");
        }

        @Test
        @DisplayName("start() from DRAFT throws")
        void start_fromDraft_throws() {
            TestWorkOrder wo = workOrderIn(DRAFT);

            IllegalStatusTransitionException ex =
                    assertThrows(IllegalStatusTransitionException.class, wo::start);

            assertArrayEquals(new Object[]{DRAFT, IN_PROGRESS}, ex.getMessageArgs());
            assertEquals(DRAFT, wo.getStatus(), "Status must not change on a forbidden transition");
        }

        @Test
        @DisplayName("complete() from CONFIRMED throws")
        void complete_fromConfirmed_throws() {
            TestWorkOrder wo = workOrderIn(CONFIRMED);

            IllegalStatusTransitionException ex =
                    assertThrows(IllegalStatusTransitionException.class, wo::complete);

            assertArrayEquals(new Object[]{CONFIRMED, COMPLETED}, ex.getMessageArgs());
            assertEquals(CONFIRMED, wo.getStatus(), "Status must not change on a forbidden transition");
        }

        @Test
        @DisplayName("cancel() from COMPLETED throws")
        void cancel_fromCompleted_throws() {
            TestWorkOrder wo = workOrderIn(COMPLETED);

            IllegalStatusTransitionException ex =
                    assertThrows(IllegalStatusTransitionException.class, wo::cancel);

            assertArrayEquals(new Object[]{COMPLETED, CANCELLED}, ex.getMessageArgs());
            assertEquals(COMPLETED, wo.getStatus(), "Status must not change on a forbidden transition");
        }

        @Test
        @DisplayName("reopen() from IN_PROGRESS throws")
        void reopen_fromInProgress_throws() {
            TestWorkOrder wo = workOrderIn(IN_PROGRESS);

            IllegalStatusTransitionException ex =
                    assertThrows(IllegalStatusTransitionException.class, wo::reopen);

            assertArrayEquals(new Object[]{IN_PROGRESS, DRAFT}, ex.getMessageArgs());
            assertEquals(IN_PROGRESS, wo.getStatus(), "Status must not change on a forbidden transition");
        }
    }

    private static TestWorkOrder workOrderIn(WorkOrderStatus target) {
        TestWorkOrder wo = new TestWorkOrder(1, 1);
        wo.addItem(createService(1, Money.of(899)));

        switch (target) {
            case DRAFT:
                break;
            case CONFIRMED:
                wo.confirm();
                break;
            case IN_PROGRESS:
                wo.confirm();
                wo.start();
                break;
            case COMPLETED:
                wo.confirm();
                wo.start();
                wo.complete();
                break;
            case CANCELLED:
                wo.cancel();
                break;
        }
        assertEquals(target, wo.getStatus(), "Arrange");
        return wo;
    }

    private static ServiceItem createService(int id, Money price) {
        ServiceItem service = mock(ServiceItem.class);

        when(service.getId()).thenReturn(id);
        when(service.getName()).thenReturn("Oil change");
        when(service.getPrice()).thenReturn(price);
        when(service.getEstimatedMinutes()).thenReturn(45);

        return service;
    }

    private static class TestWorkOrder extends WorkOrder {

        TestWorkOrder(Integer vehicleId, Integer mechanicId) {
            super(null, vehicleId, mechanicId, new ArrayList<>());
        }

        @Override
        public WorkOrderType getType() {
            return WorkOrderType.PLANNED;
        }
    }
}
