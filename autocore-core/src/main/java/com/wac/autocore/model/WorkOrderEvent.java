package com.wac.autocore.model;


import java.time.LocalDateTime;

/** A Record (immutable) class that works as a DTO ({@code Data-Transfer Object}) for
 * representing an event that has occurred on a {@link WorkOrder}.
 * It contains the work order id,type, {@link WorkOrderEventAction} and time the event occurred.
 * **/
public record WorkOrderEvent(
        Integer workOrderId,
        WorkOrderType workOrderType,
        WorkOrderEventAction action,
        LocalDateTime occurredAt
) {
    public enum WorkOrderEventAction {
        CREATED, CONFIRMED, STARTED, COMPLETED, CANCELLED, REOPENED
    }
}
