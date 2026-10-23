package com.wac.autocore.model;

import com.wac.autocore.exception.ValidationException;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.util.ArrayList;
import java.util.List;

/**
 * <b>PlannedWorkOrder</b>
 * <p>Responsibility: A work order for a planned booking. The order carries a copy of the booking's services
 * ({@link WorkOrderItem}) with name, agreed price and duration, so the workshop knows which jobs
 * are to be performed and the invoice can be produced without reading the service catalog.</p>
 * <p>Created only via {@link #createFrom(Booking)}, as a draft. The jobs do not change after the order has been created.</p>
 */
@Entity
@DiscriminatorValue("PLANNED")
public class PlannedWorkOrder extends WorkOrder {

    protected PlannedWorkOrder() {
    }

    private PlannedWorkOrder(Integer bookingId, Integer vehicleId, Integer mechanicId, List<WorkOrderItem> items) {
        super(bookingId, vehicleId, mechanicId, items);
    }

    /* Factory method. Creates a planned work order from a booking. The vehicle, date, start time and
     * all service lines are copied, with prices and durations as agreed at booking time.
     * The booking's suggested mechanic (may be missing) becomes the preselection. The order becomes a draft and
     * reserves the mechanic only when it is confirmed. */
    public static PlannedWorkOrder createFrom(Booking booking) {
        if (booking == null || booking.getItems().isEmpty()) {
            throw new ValidationException("error.workOrder.bookingHasNoServices");
        }

        List<WorkOrderItem> copiedItems = new ArrayList<>();
        for (BookingServiceItem bookingLine : booking.getItems()) {
            copiedItems.add(WorkOrderItem.from(bookingLine));
        }

        PlannedWorkOrder workOrder = new PlannedWorkOrder(booking.getId(), booking.getVehicleId(),
                booking.getMechanicId(), copiedItems);
        workOrder.setPlannedDate(booking.getDate());
        workOrder.setPlannedStartTime(booking.getStartTime());
        return workOrder;
    }

    @Override
    public WorkOrderType getType() {
        return WorkOrderType.PLANNED;
    }

    @Override
    protected void validateTypeSpecific() {
        if (getBookingId() == null) {
            throw new ValidationException("error.workOrder.missingBooking");
        }
        // The customer's time must exist before the mechanic can be reserved. Same keys as the booking.
        if (getPlannedDate() == null) {
            throw new ValidationException("error.dateSelect");
        }
        if (getPlannedStartTime() == null) {
            throw new ValidationException("error.startTimeSelect");
        }
    }
}
