package com.wac.autocore.service;

import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderChange;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

// Samlar ordern, eventuella skillnader och en eventuell krockvarning i ett resultatobjekt från WorkOrderService.reopenWorkOrder().
// Vid återöppning av en avbruten planerad order är ordern en ny order och listan visar vad som skiljer mot den avbrutna.
// I övriga fall är det samma order och listan är tom.
// Ordern och krockvarningen hålls av ett WorkOrderScheduleResult, så de beräknas och visas på samma sätt som vid skapande.
public final class WorkOrderReopenResult {

    private final WorkOrderScheduleResult schedule;
    private final List<WorkOrderChange> changes;

    // Hindrar anroparen från att ändra listan genom att skapa en oföränderlig kopia av den.
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