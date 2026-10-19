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
 * <p>Skapas bara via {@link #createFrom(Booking)}, som ett utkast. Jobben ändras inte efter att ordern skapats.</p>
 */
@Entity
@DiscriminatorValue("PLANNED")
public class PlannedWorkOrder extends WorkOrder {

    protected PlannedWorkOrder() {
    }

    private PlannedWorkOrder(Integer bookingId, Integer vehicleId, Integer mechanicId, List<WorkOrderItem> items) {
        super(bookingId, vehicleId, mechanicId, items);
    }

    /* Fabriksmetod. Skapar en planerad arbetsorder från en bokning. Fordonet, datum, starttid och
     * alla tjänsterader kopieras, med priser och tider som de avtalades vid bokningen.
     * Bokningens föreslagna mekaniker (kan saknas) blir förval. Ordern blir ett utkast och
     * reserverar mekanikern först när den bekräftas. */
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
        // Kundens tid måste finnas innan mekanikern kan reserveras. Samma nycklar som bokningen.
        if (getPlannedDate() == null) {
            throw new ValidationException("error.dateSelect");
        }
        if (getPlannedStartTime() == null) {
            throw new ValidationException("error.startTimeSelect");
        }
    }
}
