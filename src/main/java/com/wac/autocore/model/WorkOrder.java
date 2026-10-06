package com.wac.autocore.model;

import com.wac.autocore.exception.ValidationException;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <b>WorkOrder</b>
 * <p>Ansvar: En arbetsorder för en bokning. Ordern bär en kopia av bokningens tjänster
 * ({@link WorkOrderItem}) med namn, avtalat pris och tid, så att verkstaden vet vilka jobb
 * som ska utföras och fakturan kan tas fram utan att läsa tjänstekatalogen.</p>
 * <p>Skapas bara via {@link #createFrom(Booking)}. Jobben ändras inte efter att ordern skapats.</p>
 */
@Entity
@Table(name = "work_order")
public class WorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private int bookingId;
    private int mechanicId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "work_order_service_item", joinColumns = @JoinColumn(name = "work_order_id"))
    @Column(name = "service_item_id")
    private List<WorkOrderItem> items = new ArrayList<>();

    private String status = "CONFIRMED";

    private String type = "PLANNED";

    protected WorkOrder() {}

    public WorkOrder(int bookingId, int mechanicId, List<WorkOrderItem> items) {
        this.bookingId = bookingId;
        this.mechanicId = mechanicId;
        this.items.addAll(items);
    }

    /* Skapar en arbetsorder från en bokning. Mekanikern och alla tjänsterader
     * kopieras, med priser och tider som de avtalades vid bokningen. */
    public static WorkOrder createFrom(Booking booking) {
        if (booking == null || booking.getItems().isEmpty()) {
            throw new ValidationException("error.workOrderNoServices");
        }

        List<WorkOrderItem> copiedItems = new ArrayList<>();
        for (BookingServiceItem bookingLine : booking.getItems()) {
            copiedItems.add(WorkOrderItem.from(bookingLine));
        }

        return new WorkOrder(booking.getId(), booking.getMechanicId(), copiedItems);
    }

    public int getId() {
        return id;
    }

    public int getBookingId() {
        return bookingId;
    }

    public int getMechanicId() {
        return mechanicId;
    }

    public void setMechanicId(int mechanicId) {
        this.mechanicId = mechanicId;
    }

    // Skrivskyddad lista med jobben som ska utföras
    public List<WorkOrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    // Summan av de avtalade priserna, underlag för fakturan
    public double getTotalPrice() {
        return items.stream()
                .mapToDouble(WorkOrderItem::getAgreedPrice)
                .sum();
    }

    public int getTotalDurationMinutes() {
        return items.stream()
                .mapToInt(WorkOrderItem::getDurationMinutes)
                .sum();
    }

    public String getStatus() {
        return status;
    }

    public String getType() {
        return type;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return id +
                " - Booking ID: " + bookingId +
                " | Mechanic ID: " + mechanicId +
                " | Jobs: " + items.size() +
                " | Status: " + status +
                " | Type: " + type;
    }
}