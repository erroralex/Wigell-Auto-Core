package com.wac.autocore.model;

/**
 * <b>WorkOrderChangeType</b>
 * <p>Responsibility: What kind of difference {@link WorkOrderChange#between(WorkOrder, WorkOrder)} has found between two work orders.</p>
 */
public enum WorkOrderChangeType {
    VEHICLE_CHANGED,
    MECHANIC_CHANGED,
    DATE_CHANGED,
    START_TIME_CHANGED,
    SERVICE_REMOVED,
    PRICE_CHANGED,
    DURATION_CHANGED,
    SERVICE_ADDED
}
