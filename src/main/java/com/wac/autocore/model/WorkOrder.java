package com.wac.autocore.model;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

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
    private List<Integer> serviceItemIds = new ArrayList<>();

    private String status = "CREATED";

    protected WorkOrder() {}

    @Deprecated // Tillfällig tills dess att repositories ersätter Database
    public WorkOrder(int id, int bookingId, int mechanicId) {
        this(bookingId, mechanicId);
        this.id = id;
    }

    public WorkOrder(int bookingId, int mechanicId) {
        this.bookingId = bookingId;
        this.mechanicId = mechanicId;
    }

    public int getId() {
        return id;
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public int getMechanicId() {
        return mechanicId;
    }

    public void setMechanicId(int mechanicId) {
        this.mechanicId = mechanicId;
    }

    public List<Integer> getServiceItemIds() {
        return serviceItemIds;
    }

    public void setServiceItemIds(List<Integer> serviceItemIds) {
        this.serviceItemIds = serviceItemIds;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void addServiceItem(int serviceItemId) {
        if (!serviceItemIds.contains(serviceItemId)) {
            serviceItemIds.add(serviceItemId);
        }
    }

    public void removeServiceItem(int serviceItemId) {
        serviceItemIds.remove(Integer.valueOf(serviceItemId));
    }

    @Override
    public String toString() {
        return id +
                " - Booking ID: " + bookingId +
                " | Mechanic ID: " + mechanicId +
                " | Services: " + serviceItemIds +
                " | Status: " + status;
    }
}