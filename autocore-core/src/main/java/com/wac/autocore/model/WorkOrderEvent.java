package com.wac.autocore.model;


import java.time.LocalTime;

public record WorkOrderEvent(
        Integer workOrderId,
        WorkOrderType workOrderType,
        WorkOrderEventAction action,
        LocalTime occuredAt
) {
    public enum WorkOrderEventAction {
        CREATED, CONFIRMED, STARTED, COMPLETED, CANCELLED, REOPENED
    }
}
