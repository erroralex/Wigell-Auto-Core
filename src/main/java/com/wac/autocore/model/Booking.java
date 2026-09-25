package com.wac.autocore.model;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
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

    @ManyToMany
    @JoinTable(
            name = "booking_service_item",
            joinColumns = @JoinColumn(name = "booking_id"),
            inverseJoinColumns = @JoinColumn(name = "service_item_id")
    )
    private Set<ServiceItem> serviceItems = new HashSet<>();

    protected Booking() {}

/*    @Deprecated // Tillfällig tills dess att repositories ersätter Database
    public Booking(int id, int vehicleId, LocalDate date, String description) {
        this(vehicleId, 0, date, null, null, description);
        this.id = id;
    }*/

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

    public Set<ServiceItem> getServiceItems() {
        return serviceItems;
    }

    public void setServiceItems(Set<ServiceItem> serviceItems) {
        this.serviceItems = serviceItems;
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