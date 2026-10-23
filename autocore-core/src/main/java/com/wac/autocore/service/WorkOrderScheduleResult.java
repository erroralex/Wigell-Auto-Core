package com.wac.autocore.service;

import com.wac.autocore.model.WorkOrder;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

// Collects the order and any clash warning for the mechanic in a result object from WorkOrderService.
// Clashes do not stop a draft; the caller shows the warning and can change mechanic or time.
// Without clashes the list is empty and no start time suggestion is given.
public final class WorkOrderScheduleResult {

    private final WorkOrder workOrder;
    private final List<WorkOrder> mechanicConflicts;
    private final Optional<LocalTime> suggestedStartTime;

    // Prevents the caller from changing the list by creating an immutable copy of it.
    public WorkOrderScheduleResult(WorkOrder workOrder, List<WorkOrder> mechanicConflicts,
                                   Optional<LocalTime> suggestedStartTime) {
        this.workOrder = workOrder;
        this.mechanicConflicts = Collections.unmodifiableList(new ArrayList<>(mechanicConflicts));
        this.suggestedStartTime = suggestedStartTime;
    }

    public static WorkOrderScheduleResult withoutConflicts(WorkOrder workOrder) {
        return new WorkOrderScheduleResult(workOrder, Collections.<WorkOrder>emptyList(), Optional.<LocalTime>empty());
    }

    public WorkOrder getWorkOrder() {
        return workOrder;
    }

    public List<WorkOrder> getMechanicConflicts() {
        return mechanicConflicts;
    }

    public Optional<LocalTime> getSuggestedStartTime() {
        return suggestedStartTime;
    }
}