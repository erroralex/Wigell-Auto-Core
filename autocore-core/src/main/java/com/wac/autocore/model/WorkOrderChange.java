package com.wac.autocore.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <b>WorkOrderChange</b>
 * <p>Responsibility: A difference between two work orders, e.g. a cancelled order and the new order created
 * from the booking on reopening. Immutable value object.</p>
 * <p>Changes to a service carry the lines before and after ({@code null} when the line is missing on one side).
 * A change of mechanic or vehicle carries the ids before and after, a move of date or start time carries the values before and after.
 * The view translates ids to names and formats the amounts.</p>
 * <p>{@link #between(WorkOrder, WorkOrder)} compares two work orders and lists what differs: vehicle, mechanic,
 * date, start time, removed services, changed agreed price or duration and added services. The lines are matched on the service id.
 * Used on reopening to inform about what has changed in the booking since the order was cancelled.
 * The comparison blocks nothing.</p>
 */
public final class WorkOrderChange {

    private final WorkOrderChangeType type;
    private final WorkOrderItem previousItem;
    private final WorkOrderItem currentItem;
    private final Integer previousId;
    private final Integer currentId;
    private final LocalDate previousDate;
    private final LocalDate currentDate;
    private final LocalTime previousStartTime;
    private final LocalTime currentStartTime;

    private WorkOrderChange(WorkOrderChangeType type,
                            WorkOrderItem previousItem, WorkOrderItem currentItem,
                            Integer previousId, Integer currentId) {
        this(type, previousItem, currentItem, previousId, currentId, null, null, null, null);
    }

    private WorkOrderChange(WorkOrderChangeType type,
                            WorkOrderItem previousItem, WorkOrderItem currentItem,
                            Integer previousId, Integer currentId,
                            LocalDate previousDate, LocalDate currentDate,
                            LocalTime previousStartTime, LocalTime currentStartTime) {
        this.type = type;
        this.previousItem = previousItem;
        this.currentItem = currentItem;
        this.previousId = previousId;
        this.currentId = currentId;
        this.previousDate = previousDate;
        this.currentDate = currentDate;
        this.previousStartTime = previousStartTime;
        this.currentStartTime = currentStartTime;
    }

    // The order is fixed: vehicle, mechanic, date, start time, the previous lines in their order and finally the added services.
    public static List<WorkOrderChange> between(WorkOrder previous, WorkOrder current) {
        Objects.requireNonNull(previous, "previous must not be null");
        Objects.requireNonNull(current, "current must not be null");

        List<WorkOrderChange> changes = new ArrayList<>();

        if (!Objects.equals(previous.getVehicleId(), current.getVehicleId())) {
            changes.add(vehicleChanged(previous.getVehicleId(), current.getVehicleId()));
        }
        if (!Objects.equals(previous.getMechanicId(), current.getMechanicId())) {
            changes.add(mechanicChanged(previous.getMechanicId(), current.getMechanicId()));
        }
        if (!Objects.equals(previous.getPlannedDate(), current.getPlannedDate())) {
            changes.add(new WorkOrderChange(WorkOrderChangeType.DATE_CHANGED, null, null, null, null,
                    previous.getPlannedDate(), current.getPlannedDate(), null, null));
        }
        if (!Objects.equals(previous.getPlannedStartTime(), current.getPlannedStartTime())) {
            changes.add(new WorkOrderChange(WorkOrderChangeType.START_TIME_CHANGED, null, null, null, null,
                    null, null, previous.getPlannedStartTime(), current.getPlannedStartTime()));
        }

        Map<Integer, WorkOrderItem> notMatched = new LinkedHashMap<>();
        for (WorkOrderItem item : current.getItems()) {
            notMatched.put(item.getServiceItemId(), item);
        }

        for (WorkOrderItem before : previous.getItems()) {
            WorkOrderItem after = notMatched.remove(before.getServiceItemId());
            if (after == null) {
                changes.add(serviceRemoved(before));
                continue;
            }
            if (!before.getAgreedPrice().equals(after.getAgreedPrice())) {
                changes.add(priceChanged(before, after));
            }
            if (before.getDurationMinutes() != after.getDurationMinutes()) {
                changes.add(durationChanged(before, after));
            }
        }

        for (WorkOrderItem added : notMatched.values()) {
            changes.add(serviceAdded(added));
        }

        return Collections.unmodifiableList(changes);
    }

    private static WorkOrderChange vehicleChanged(Integer previousId, Integer currentId) {
        return new WorkOrderChange(WorkOrderChangeType.VEHICLE_CHANGED, null, null, previousId, currentId);
    }

    private static WorkOrderChange mechanicChanged(Integer previousId, Integer currentId) {
        return new WorkOrderChange(WorkOrderChangeType.MECHANIC_CHANGED, null, null, previousId, currentId);
    }

    private static WorkOrderChange serviceRemoved(WorkOrderItem previousItem) {
        return new WorkOrderChange(WorkOrderChangeType.SERVICE_REMOVED, previousItem, null, null, null);
    }

    private static WorkOrderChange serviceAdded(WorkOrderItem currentItem) {
        return new WorkOrderChange(WorkOrderChangeType.SERVICE_ADDED, null, currentItem, null, null);
    }

    private static WorkOrderChange priceChanged(WorkOrderItem previousItem, WorkOrderItem currentItem) {
        return new WorkOrderChange(WorkOrderChangeType.PRICE_CHANGED, previousItem, currentItem, null, null);
    }

    private static WorkOrderChange durationChanged(WorkOrderItem previousItem, WorkOrderItem currentItem) {
        return new WorkOrderChange(WorkOrderChangeType.DURATION_CHANGED, previousItem, currentItem, null, null);
    }

    public WorkOrderChangeType getType() {
        return type;
    }

    // The name of the service the change concerns, or null for a change of mechanic or vehicle
    public String getServiceName() {
        if (currentItem != null) {
            return currentItem.getServiceName();
        }
        return previousItem == null ? null : previousItem.getServiceName();
    }

    public WorkOrderItem getPreviousItem() {
        return previousItem;
    }

    public WorkOrderItem getCurrentItem() {
        return currentItem;
    }

    public Integer getPreviousId() {
        return previousId;
    }

    public Integer getCurrentId() {
        return currentId;
    }

    public LocalDate getPreviousDate() {
        return previousDate;
    }

    public LocalDate getCurrentDate() {
        return currentDate;
    }

    public LocalTime getPreviousStartTime() {
        return previousStartTime;
    }

    public LocalTime getCurrentStartTime() {
        return currentStartTime;
    }
}
