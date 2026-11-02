package com.wac.autocore.model;

import com.wac.autocore.exception.ValidationException;
import java.util.Collections;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * <b>DropInWorkOrder</b>
 * <p>Responsibility: A work order for a car that comes into the workshop without a booking.
 * The order therefore has no link to a {@link Booking} ({@code bookingId} is always {@code null}).</p>
 * <p>Created only via {@link #draft(int, String)} with vehicle and problem description, and starts
 * as a draft ({@link WorkOrderStatus#DRAFT}). Mechanic and services are added afterwards
 * ({@link #setMechanicId(Integer)}, {@link #addItem(ServiceItem)}).
 * The order can be confirmed with {@link #confirm()} only once the common fields are filled in and
 * the problem description is still present, because {@link WorkOrderType#DROP_IN} requires one.</p>
 */
@Entity
@DiscriminatorValue("DROP_IN")
public class DropInWorkOrder extends WorkOrder {

    protected DropInWorkOrder() {
    }

    private DropInWorkOrder(int vehicleId, String problemDescription) {
        super(
                null,
                vehicleId,
                null,
                Collections.emptyList()
        );

        setProblemDescription(problemDescription);
    }

    public static DropInWorkOrder draft(int vehicleId, String problemDescription) {

        if (vehicleId <= 0) {
            throw new ValidationException("error.workOrder.missingVehicle");
        }

        requireProblemDescription(WorkOrderType.DROP_IN, problemDescription);

        return new DropInWorkOrder(vehicleId, problemDescription);

    }

    @Override
    public WorkOrderType getType() {
        return WorkOrderType.DROP_IN;
    }
}
