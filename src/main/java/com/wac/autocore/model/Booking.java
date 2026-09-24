package com.wac.autocore.model;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "booking")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private int vehicleId;
    private int mechanicId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String description;
    private String status = "BOOKED";

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