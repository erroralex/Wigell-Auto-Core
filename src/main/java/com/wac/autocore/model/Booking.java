package com.wac.autocore.model;

import com.wac.autocore.exception.BookingLockedException;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <b>Booking</b>
 * <p>Ansvar: En bokning av ett fordon hos en mekaniker. Bokningen äger sina tjänsterader
 * ({@link BookingServiceItem}) där namn, pris och tid frystes när tjänsten lades till.</p>
 * <p>Rader läggs bara till via {@link #addServiceItem(ServiceItem)}, som kontrollerar att
 * bokningen fortfarande går att ändra.</p>
 */
@Entity
@Table(name = "booking")

// Implementerar BookingPrototype för att kunna skapa en kopia av bokningen med nya datum och starttid, enligt prototypmönstret.
public class Booking implements BookingPrototype {

    public static final String STATUS_BOOKED = "BOOKED";

    // Implementerar copyAsNew-metoden från BookingPrototype
    // för att skapa en ny instans av Booking med samma data, men med nytt datum och starttid.
    @Override
    public Booking copyAsNew(LocalDate date, LocalTime startTime) {

        // Beräknar ny sluttid baserat på starttid och total varaktighet från tjänsterna.
        LocalTime newEndTime = startTime.plusMinutes(getTotalDurationMinutes());

        Booking copy = new Booking(
                this.vehicleId,
                this.mechanicId,
                date,
                startTime,
                newEndTime,
                this.description
        );

        // Läser in orginalets lista och kopierar varje tjänsterad till den nya bokningen.
        for (BookingServiceItem item : this.items) {
            BookingServiceItem copiedItem = item.copyAsNew();
            copiedItem.setBooking(copy);
            copy.items.add(copiedItem);

        }
        return copy; //Prototype: "originalet vet hur det skapar en självständig kopia av sig själv"

    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "vehicle_id", nullable = false)
    private int vehicleId;

    @Column(name = "mechanic_id", nullable = false)
    private int mechanicId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String status = STATUS_BOOKED;

    @OneToMany(
            mappedBy = "booking",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    @OrderBy("id ASC")
    private List<BookingServiceItem> items = new ArrayList<>();

    protected Booking() {
    }

    public Booking(int vehicleId, int mechanicId, LocalDate date,
                   LocalTime startTime, LocalTime endTime, String description) {
        this.vehicleId = vehicleId;
        this.mechanicId = mechanicId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public int getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(int vehicleId) {
        this.vehicleId = vehicleId;
    }

    public int getMechanicId() {
        return mechanicId;
    }

    public void setMechanicId(int mechanicId) {
        this.mechanicId = mechanicId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /* Lägger till en tjänst som en frusen snapshot-rad.
     * Samma tjänst läggs inte till två gånger. Kastar BookingLockedException
     * om arbetet redan har påbörjats. */
    public void addServiceItem(ServiceItem serviceItem) {
        if (!STATUS_BOOKED.equals(status)) {
            throw new BookingLockedException(id, status);
        }

        BookingServiceItem item = BookingServiceItem.snapshotOf(serviceItem);
        if (containsService(item.getServiceItemId())) {
            return;
        }

        item.setBooking(this);
        items.add(item);
    }

    public List<BookingServiceItem> getItems() {
        return items;
    }

    private boolean containsService(int serviceItemId) {
        for (BookingServiceItem item : items) {
            if (item.getServiceItemId() == serviceItemId) {
                return true;
            }
        }
        return false;
    }

    public double getTotalAgreedPrice() {
        return items.stream()
                .mapToDouble(BookingServiceItem::getPriceAtBooking)
                .sum();
    }

    public int getTotalDurationMinutes() {
        return items.stream()
                .mapToInt(BookingServiceItem::getDurationMinutes)
                .sum();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void removeServiceItem(int serviceItemId) {
        if (!STATUS_BOOKED.equals(status)) {
            throw new BookingLockedException(id, status);
        }

        items.removeIf(item -> item.getServiceItemId() == serviceItemId);
    }

    @Override
    public String toString() {
        return id + " - Vehicle ID: " + vehicleId +
                " | Mechanic ID: " + mechanicId +
                " | Date: " + date + " " + startTime + "-" + endTime +
                " | Description: " + description +
                " | Status: " + status;
    }

}
