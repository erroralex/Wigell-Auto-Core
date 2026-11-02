package com.wac.autocore.service;

import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderChange;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

// Collects the order, any differences and any clash warning in a result object from WorkOrderService.reopenWorkOrder().
// When reopening a cancelled planned order, the order is a new order and the list shows what differs from the cancelled one.
// In all other cases it is the same order and the list is empty.
// The order and the clash warning are held by a WorkOrderScheduleResult, so they are calculated and shown the same way as on creation.
public final class WorkOrderReopenResult {

    private final WorkOrderScheduleResult schedule;
    private final List<WorkOrderChange> changes;

    // Prevents the caller from changing the list by creating an immutable copy of it.
    public WorkOrderReopenResult(WorkOrderScheduleResult schedule, List<WorkOrderChange> changes) {
        this.schedule = schedule;
        this.changes = Collections.unmodifiableList(new ArrayList<>(changes));
    }

    public WorkOrderScheduleResult getSchedule() {
        return schedule;
    }

    public WorkOrder getWorkOrder() {
        return schedule.getWorkOrder();
    }

    public List<WorkOrderChange> getChanges() {
        return changes;
    }

    public List<WorkOrder> getMechanicConflicts() {
        return schedule.getMechanicConflicts();
    }

    public Optional<LocalTime> getSuggestedStartTime() {
        return schedule.getSuggestedStartTime();
    }
}