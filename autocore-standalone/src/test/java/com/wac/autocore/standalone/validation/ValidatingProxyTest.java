package com.wac.autocore.standalone.validation;

import com.wac.autocore.exception.handler.ErrorHandlerChain;
import com.wac.autocore.model.DropInWorkOrder;
import com.wac.autocore.service.WorkOrderLifecycle;
import com.wac.autocore.service.WorkOrderScheduleResult;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidatingProxyTest {

    @Test
    @DisplayName("An invalid id is rejected before the service is called, with the same error key as the app")
    void invalidIdIsRejectedBeforeTheServiceIsCalled() {
        RecordingLifecycle service = new RecordingLifecycle();
        WorkOrderLifecycle proxy = ValidatingProxy.of(WorkOrderLifecycle.class, service);

        ConstraintViolationException error =
                assertThrows(ConstraintViolationException.class, () -> proxy.confirmWorkOrder(0));

        assertEquals(0, service.calls);
        assertEquals("error.fields", ErrorHandlerChain.createDefault().resolve(error).getMessageKey());
    }

    @Test
    void validCallReachesTheService() {
        RecordingLifecycle service = new RecordingLifecycle();
        WorkOrderLifecycle proxy = ValidatingProxy.of(WorkOrderLifecycle.class, service);

        proxy.confirmWorkOrder(1);

        assertEquals(1, service.calls);
    }

    // Counts calls so the test can see whether the proxy let the call through.
    private static final class RecordingLifecycle implements WorkOrderLifecycle {
        int calls;

        @Override
        public DropInWorkOrder createDropIn(int vehicleId, String problemDescription, List<Integer> serviceItemIds) {
            calls++;
            return null;
        }

        @Override
        public WorkOrderScheduleResult updateWorkOrder(int id, List<Integer> serviceItemIds, Integer mechanicId,
                                                       LocalDate plannedDate, LocalTime plannedStartTime,
                                                       String customerInstructions, String comments,
                                                       Map<Integer, Boolean> chargeFlags) {
            calls++;
            return null;
        }

        @Override
        public void confirmWorkOrder(int id) {
            calls++;
        }

        @Override
        public void startWorkOrder(int id) {
            calls++;
        }

        @Override
        public void completeWorkOrder(int id) {
            calls++;
        }

        @Override
        public void cancelWorkOrder(int id) {
            calls++;
        }
    }
}
