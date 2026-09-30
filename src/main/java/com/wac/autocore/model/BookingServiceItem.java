package com.wac.autocore.model;

import com.wac.autocore.service.LanguageManager;
import javax.persistence.*;

@Entity
@Table(name = "booking_service_item", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_booking_service_item_booking_service",
                columnNames = {"booking_id", "service_item_id"}
        )
})

public class BookingServiceItem {

    private static final LanguageManager lang = LanguageManager.getInstance();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "service_item_id", nullable = false)
    private int serviceItemId;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(name = "price_at_booking", nullable = false)
    private double priceAtBooking;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    protected BookingServiceItem() {}

    private BookingServiceItem(int serviceItemId,
                               String serviceName,
                               double priceAtBooking,
                               int durationMinutes) {
        this.serviceItemId = serviceItemId;
        this.serviceName = serviceName;
        this.priceAtBooking = priceAtBooking;
        this.durationMinutes = durationMinutes;
    }

    public static BookingServiceItem snapshotOf(ServiceItem serviceItem) {
        if (serviceItem == null) {
            throw new IllegalArgumentException(lang.get("error.serviceSelect"));
        }

        return new BookingServiceItem(
                serviceItem.getId(),
                serviceItem.getName(),
                serviceItem.getPrice(),
                serviceItem.getEstimatedMinutes()
        );
    }

    void setBooking(Booking booking) {
        this.booking = booking;
    }

    public int getId() {
        return id;
    }

    public Booking getBooking() {
        return booking;
    }

    public int getServiceItemId() {
        return serviceItemId;
    }

    public String getServiceName() {
        return serviceName;
    }

    /** Price frozen at booking time. */
    public double getPriceAtBooking() {
        return priceAtBooking;
    }

    /** Duration frozen at booking time. */
    public int getDurationMinutes() {
        return durationMinutes;
    }

    @Override
    public String toString() {
        return serviceName + " | Price: " + priceAtBooking + " SEK" +
                " | Estimated time: " + durationMinutes + " min";
    }

}
