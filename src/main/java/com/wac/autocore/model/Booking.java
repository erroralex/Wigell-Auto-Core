package com.wac.autocore.model;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "booking")
public class Booking {

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
    private String status = "BOOKED";

    @OneToMany(
            mappedBy = "booking",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    @OrderBy("id ASC")
    private List<BookingServiceItem> items = new ArrayList<>();

    protected Booking() {}

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

    protected void setVehicleId(int vehicleId) {
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

    public void addServiceItem(ServiceItem serviceItem) {
        BookingServiceItem item = BookingServiceItem.snapshotOf(serviceItem);
        item.setBooking(this);
        items.add(item);
    }

    public List<BookingServiceItem> getItems() {
        return items;
    }

    public double getTotalEstimatedPrice() {
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

    @Override
    public String toString() {
        return id + " - Vehicle ID: " + vehicleId +
                " | Mechanic ID: " + mechanicId +
                " | Date: " + date + " " + startTime + "-" + endTime +
                " | Description: " + description +
                " | Status: " + status;
    }
}