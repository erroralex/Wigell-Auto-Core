package com.wac.autocore.service;

import com.wac.autocore.model.WorkOrder;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

// Samlar ordern och en eventuell krockvarning för mekanikern i ett resultatobjekt från WorkOrderService.
// Krockar stoppar inte ett utkast; anroparen visar varningen och kan byta mekaniker eller tid.
// Utan krockar är listan tom och inget förslag på starttid ges.
public final class WorkOrderScheduleResult {

    private final WorkOrder workOrder;
    private final List<WorkOrder> mechanicConflicts;
    private final Optional<LocalTime> suggestedStartTime;

    // Hindrar anroparen från att ändra listan genom att skapa en oföränderlig kopia av den.
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