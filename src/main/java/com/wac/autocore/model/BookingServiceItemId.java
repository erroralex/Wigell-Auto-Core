package com.wac.autocore.model;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class BookingServiceItemId implements Serializable {

    @Column(name = "booking_id")
    private int bookingId;

    @Column(name = "service_item_id")
    private int serviceItemId;

    public BookingServiceItemId() {}

    public BookingServiceItemId(int bookingId, int serviceItemId) {
        this.bookingId = bookingId;
        this.serviceItemId = serviceItemId;
    }

    public int getBookingId() { return bookingId; }
    public void setBookingId(int bookingId) { this.bookingId = bookingId; }
    public int getServiceItemId() { return serviceItemId; }
    public void setServiceItemId(int serviceItemId) { this.serviceItemId = serviceItemId; }

    //Krävs av JPA för sammansatt(Composite) nyckel.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookingServiceItemId that = (BookingServiceItemId) o;
        return bookingId == that.bookingId && serviceItemId == that.serviceItemId;
    }

    public int hashCode() {
        return Objects.hash(bookingId, serviceItemId);
    }
}
