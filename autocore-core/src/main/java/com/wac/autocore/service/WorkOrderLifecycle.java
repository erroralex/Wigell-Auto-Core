package com.wac.autocore.service;

import com.wac.autocore.model.DropInWorkOrder;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * <b>WorkOrderLifecycle</b>
 * <p>Responsibility: The work order operations a scenario needs: create a drop-in order,
 * edit it and move it through the lifecycle (confirm, start, complete, cancel).</p>
 * <p>Plays the <b>Subject</b> role in the <b>Proxy</b> pattern (GoF). {@link WorkOrderService}
 * is the real subject, and a protection proxy can stand in front of it to check access
 * before each call. Clients such as {@code DropInScenario} depend only on this interface,
 * so they work the same with or without the proxy.</p>
 * <p>Kept narrow on purpose: only the operations a scenario uses,
 * not every method of {@link WorkOrderService}.</p>
 * <p>Parameter constraints are declared here, not in the implementation, because Bean
 * Validation does not allow an overriding method to redefine them.</p>
 */
public interface WorkOrderLifecycle {

    DropInWorkOrder createDropIn(@Positive(message = "error.notFound") int vehicleId,
                                 String problemDescription,
                                 List<Integer> serviceItemIds);

    WorkOrderScheduleResult updateWorkOrder(@Positive int id,
                                            List<@NotNull @Positive Integer> serviceItemIds,
                                            @Positive Integer mechanicId,
                                            LocalDate plannedDate,
                                            LocalTime plannedStartTime,
                                            String customerInstructions,
                                            String comments,
                                            Map<Integer, Boolean> chargeFlags);

    void confirmWorkOrder(@Positive int id);
    void startWorkOrder(@Positive int id);
    void completeWorkOrder(@Positive int id);
    void cancelWorkOrder(@Positive int id);
}
