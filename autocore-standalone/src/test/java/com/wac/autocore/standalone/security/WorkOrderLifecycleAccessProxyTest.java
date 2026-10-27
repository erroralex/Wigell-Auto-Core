package com.wac.autocore.standalone.security;

import com.wac.autocore.exception.PermissionDeniedException;
import com.wac.autocore.exception.handler.ErrorHandlerChain;
import com.wac.autocore.exception.handler.ErrorReport;
import com.wac.autocore.model.DropInWorkOrder;
import com.wac.autocore.security.Role;
import com.wac.autocore.service.WorkOrderLifecycle;
import com.wac.autocore.service.WorkOrderScheduleResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorkOrderLifecycleAccessProxyTest {

    // Records which calls reach the real subject, so the test can see what the proxy let through.
    private static final class RecordingLifecycle implements WorkOrderLifecycle {
        final List<String> calls = new ArrayList<>();

        @Override
        public DropInWorkOrder createDropIn(int vehicleId, String problemDescription, List<Integer> serviceItemIds) {
            calls.add("createDropIn " + vehicleId);
            return null;
        }

        @Override
        public WorkOrderScheduleResult updateWorkOrder(
                int id,
                List<Integer> serviceItemIds,
                Integer mechanicId,
                LocalDate plannedDate,
                LocalTime plannedStartTime,
                String customerInstructions,
                String comments,
                Map<Integer, Boolean> chargeFlags) {

            calls.add("updateWorkOrder " + id);
            return null;
        }

        @Override
        public void confirmWorkOrder(int id) {
            calls.add("confirm " + id);
        }

        @Override
        public void startWorkOrder(int id) {
            calls.add("start " + id);
        }

        @Override
        public void completeWorkOrder(int id) {
            calls.add("complete " + id);
        }

        @Override
        public void cancelWorkOrder(int id) {
            calls.add("cancel " + id);
        }
    }

    @Test
    void workshopRoleIsLetThroughToTheService() {
        RecordingLifecycle target = new RecordingLifecycle();
        WorkOrderLifecycle proxy = new WorkOrderLifecycleAccessProxy(target, Role.WORKSHOP);

        proxy.completeWorkOrder(7);

        assertEquals(List.of("complete 7"), target.calls);
    }

    @Test
    void readOnlyRoleIsDeniedBeforeTheServiceIsCalled() {
        RecordingLifecycle target = new RecordingLifecycle();
        WorkOrderLifecycle proxy = new WorkOrderLifecycleAccessProxy(target, Role.READ_ONLY);

        PermissionDeniedException e = assertThrows(PermissionDeniedException.class,
                () -> proxy.createDropIn(1, "test", List.of()));

        assertEquals("error.accessDenied", e.getMessageKey());
        assertTrue(target.calls.isEmpty());
    }

    @Test
    void nullRoleIsRejected() {
        assertThrows(NullPointerException.class,
                () -> new WorkOrderLifecycleAccessProxy(new RecordingLifecycle(), null));
    }

    @Test
    void denialIsReportedAsAnExpectedError() {
        RecordingLifecycle target = new RecordingLifecycle();
        WorkOrderLifecycle proxy = new WorkOrderLifecycleAccessProxy(target, Role.READ_ONLY);

        PermissionDeniedException e = assertThrows(PermissionDeniedException.class,
                () -> proxy.completeWorkOrder(7));
        ErrorReport report = ErrorHandlerChain.createDefault().resolve(e);

        assertFalse(report.isUnexpected());
        assertEquals("error.title", report.getTitleKey());
        assertEquals("error.accessDenied", report.getMessageKey());
        assertEquals(0, report.getMessageArgs().length);
    }
}
