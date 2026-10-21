package com.wac.autocore.model;

/**
 * <b>WorkOrderChangeType</b>
 * <p>Ansvar: Vilken sorts skillnad {@link WorkOrderChange#between(WorkOrder, WorkOrder)} har hittat mellan två arbetsordrar.</p>
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
