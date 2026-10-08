package com.wac.autocore.model;

import com.wac.autocore.exception.ValidationException;
import java.util.Collections;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * <b>DropInWorkOrder</b>
 * <p>Ansvar: En arbetsorder för en bil som kommer in till verkstaden utan bokning.
 * Ordern har därför ingen koppling till en {@link Booking} ({@code bookingId} är alltid {@code null}).</p>
 * <p>Skapas bara via {@link #draft(int, String)} med fordon och problembeskrivning, och börjar
 * som utkast ({@link WorkOrderStatus#DRAFT}). Mekaniker och tjänster läggs till i efterhand.
 * Ordern kan bekräftas med {@link #confirm()} först när de gemensamma fälten är ifyllda och
 * problembeskrivningen finns kvar.</p>
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

        requireProblemDescription(problemDescription);

        return new DropInWorkOrder(vehicleId, problemDescription);

    }

    @Override
    protected void validateTypeSpecific() {
        requireProblemDescription(getProblemDescription());
    }

    // Hjälp-metod
    private static void requireProblemDescription(String problemDescription) {
        if (problemDescription == null || problemDescription.trim().isEmpty()) {
            throw new ValidationException("error.workOrder.missingProblemDescription");
        }
    }
}
