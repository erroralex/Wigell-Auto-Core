package com.wac.autocore.model;

import com.wac.autocore.exception.PricingIntegrityException;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.util.Objects;

/**
 * <b>WorkOrderItem</b>
 * <p>Ansvar: Ett jobb på en arbetsorder. Namn, avtalat pris och tidsåtgång kopieras från
 * bokningens rad ({@link BookingServiceItem}) när arbetsordern skapas och ändras aldrig
 * efter det.</p>
 * <p>Raden är ett värdeobjekt: den har ingen egen identitet och lever bara som en del av
 * sin {@link WorkOrder}. Skapas via {@link #from(BookingServiceItem)}.</p>
 */
@Embeddable
public class WorkOrderItem {

    @Column(name = "service_item_id", nullable = false)
    private int serviceItemId;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(name = "agreed_price", nullable = false)
    private double agreedPrice;

    @Column(name = "chargeable")
    private boolean chargeable = true;


    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    protected WorkOrderItem() {}

    private WorkOrderItem(int serviceItemId,
                          String serviceName,
                          double agreedPrice,
                          int durationMinutes) {
        this.serviceItemId = serviceItemId;
        this.serviceName = serviceName;
        this.agreedPrice = agreedPrice;
        this.durationMinutes = durationMinutes;
    }

    /* Kopierar en bokningsrad till ett jobb på arbetsordern. Priset hämtas från
     * bokningens snapshot, aldrig från tjänstekatalogen. */
    public static WorkOrderItem from(BookingServiceItem bookingLine) {
        if (bookingLine == null) {
            throw new PricingIntegrityException("Booking line is missing");
        }
        if (bookingLine.getServiceName() == null || bookingLine.getServiceName().trim().isEmpty()
                || bookingLine.getPriceAtBooking() < 0
                || bookingLine.getDurationMinutes() < 0) {
            throw new PricingIntegrityException(
                    "Booking line " + bookingLine.getId() + " has an invalid snapshot");
        }

        return new WorkOrderItem(
                bookingLine.getServiceItemId(),
                bookingLine.getServiceName(),
                bookingLine.getPriceAtBooking(),
                bookingLine.getDurationMinutes()
        );
    }

    public int getServiceItemId() {
        return serviceItemId;
    }

    public String getServiceName() {
        return serviceName;
    }

    /* Pris som avtalades vid bokningen. */
    public double getAgreedPrice() {
        return agreedPrice;
    }

    public boolean isChargeable() {
        return this.chargeable;
    }

    /* Tidsåtgång som avtalades vid bokningen. */
    public int getDurationMinutes() {
        return durationMinutes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkOrderItem that = (WorkOrderItem) o;
        return serviceItemId == that.serviceItemId
                && Double.compare(that.agreedPrice, agreedPrice) == 0
                && durationMinutes == that.durationMinutes
                && Objects.equals(serviceName, that.serviceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceItemId, serviceName, agreedPrice, durationMinutes);
    }

    @Override
    public String toString() {
        return serviceName + " | Price: " + agreedPrice + " SEK" +
                " | Estimated time: " + durationMinutes + " min";
    }
}
