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

    // Översätter arbetsorderns status till bokningens status.
    // CANCELLED → BOOKED: en avbruten arbetsorder släpper bokningen så den kan få en ny.
    // Alla WorkOrderStatus-värden måste mappas; annars kastas IllegalArgumentException.
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