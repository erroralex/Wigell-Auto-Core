package com.wac.autocore.model;

import com.wac.autocore.exception.ValidationException;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.util.ArrayList;
import java.util.List;

/**
 * <b>PlannedWorkOrder</b>
 * <p>Ansvar: En arbetsorder för en planerad bokning. Ordern bär en kopia av bokningens tjänster
 * ({@link WorkOrderItem}) med namn, avtalat pris och tid, så att verkstaden vet vilka jobb
 * som ska utföras och fakturan kan tas fram utan att läsa tjänstekatalogen.</p>
 * <p>Skapas bara via {@link #createFrom(Booking)}. Jobben ändras inte efter att ordern skapats.</p>
 */
@Entity
@DiscriminatorValue("PLANNED")
public class PlannedWorkOrder extends WorkOrder {

    protected PlannedWorkOrder() {
    }

    private PlannedWorkOrder(Integer bookingId, Integer vehicleId, Integer mechanicId, List<WorkOrderItem> items) {
        super(bookingId, vehicleId, mechanicId, items);
    }

    /* Fabriksmetod. Skapar en planerad arbetsorder från en bokning. Fordonet, mekanikern och
     * alla tjänsterader kopieras, med priser och tider som de avtalades vid bokningen.
     * Ordern bekräftas direkt, så att den alltid lämnar fabriken validerad och i status CONFIRMED. */
    public static PlannedWorkOrder createFrom(Booking booking) {
        if (booking == null || booking.getItems().isEmpty()) {
            throw new ValidationException("error.workOrderNoServices");
        }

        List<WorkOrderItem> copiedItems = new ArrayList<>();
        for (BookingServiceItem bookingLine : booking.getItems()) {
            copiedItems.add(WorkOrderItem.from(bookingLine));
        }

        PlannedWorkOrder workOrder = new PlannedWorkOrder(
                booking.getId(), booking.getVehicleId(), booking.getMechanicId(), copiedItems);
        workOrder.confirm();
        return workOrder;
    }

    @Override
    public WorkOrderType getType() {
        return WorkOrderType.PLANNED;
    }

    @Override
    protected void validateTypeSpecific() {
        if (getBookingId() == null) {
            throw new ValidationException("error.workOrderMissingBooking");
        }
    }
}
