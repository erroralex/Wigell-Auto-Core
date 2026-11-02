package com.wac.autocore.model;

public enum BookingStatus {

    BOOKED,
    WORK_ORDER_CREATED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    public boolean isEditable() {
        return this == BOOKED;
    }

    // Translates the work order's status to the booking's status.
    // CANCELLED → BOOKED: a cancelled work order releases the booking so it can get a new one.
    // All WorkOrderStatus values must be mapped; otherwise an IllegalArgumentException is thrown.
    public static BookingStatus forWorkOrder(WorkOrderStatus workOrderStatus) {
        switch (workOrderStatus) {
            case DRAFT:
            case CONFIRMED:
                return WORK_ORDER_CREATED;
            case IN_PROGRESS:
                return IN_PROGRESS;
            case COMPLETED:
                return COMPLETED;
            case CANCELLED:
                return BOOKED;
            default:
                throw new IllegalArgumentException("Unhandled WorkOrderStatus: " + workOrderStatus);
        }
    }
}