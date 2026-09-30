package com.wac.autocore.model;

import org.hibernate.annotations.Table;

import javax.persistence.*;

@Entity
@Table(name = "booking_service_item")
public class BookingServiceItem {

    @EmbeddedId
    private BookingServiceItemId id;

    @ManyToOne
    @MapsId("bookingId")
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @ManyToOne
    @MapsId("serviceItemId")
    @JoinColumn(name = "service_item_id")
    private ServiceItem serviceItem;

    @Column(nullable = false)
    private double price;

    protected BookingServiceItem() {}

    public BookingServiceItem(Booking booking, ServiceItem serviceItem) {
        this.booking = booking;
        this.serviceItem = serviceItem;
        this.price = serviceItem.getPrice();
    }

    public Booking getBooking() {
        return booking;
    }

    public ServiceItem serviceItem() {
        return serviceItem;
    }

    public double getPrice() {
        return price;
    }

}
