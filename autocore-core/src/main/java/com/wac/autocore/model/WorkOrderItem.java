package com.wac.autocore.model;

import com.wac.autocore.data.MoneyConverter;
import com.wac.autocore.exception.PricingIntegrityException;
import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.value.Money;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * <b>WorkOrderItem</b>
 * <p>Responsibility: A job on a work order. Name, agreed price and duration are copied from
 * the booking's line ({@link BookingServiceItem}) when the work order is created, or from a catalog service.</p>
 * <p>The line is a value object: it has no identity of its own and lives only as part of
 * its {@link WorkOrder}. Created via {@link #from(BookingServiceItem)} or {@link #snapshotOf(ServiceItem)}.</p>
 */
@Embeddable
public class WorkOrderItem {

    @Column(name = "service_item_id", nullable = false)
    private int serviceItemId;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(name = "agreed_price", nullable = false)
    @Convert(converter = MoneyConverter.class)
    private Money agreedPrice;


    @Column(name = "chargeable", columnDefinition = "INTEGER")
    private boolean chargeable = true;


    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    protected WorkOrderItem() {
    }

    private WorkOrderItem(int serviceItemId,
                          String serviceName,
                          Money agreedPrice,
                          int durationMinutes) {
        this.serviceItemId = serviceItemId;
        this.serviceName = serviceName;
        this.agreedPrice = agreedPrice;
        this.durationMinutes = durationMinutes;
    }

    /* Copies a booking line to a job on the work order. The price is taken from
     * the booking's snapshot, never from the service catalog. */
    public static WorkOrderItem from(BookingServiceItem bookingLine) {
        if (bookingLine == null) {
            throw new PricingIntegrityException("Booking line is missing");
        }
        if (bookingLine.getServiceName() == null || bookingLine.getServiceName().trim().isEmpty()
                || bookingLine.getPriceAtBooking() == null || bookingLine.getPriceAtBooking().isNegative()
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

    public static WorkOrderItem snapshotOf(ServiceItem service) {
        if (service == null) {
            throw new ValidationException("error.serviceSelect");
        }

        if (service.getId() <= 0) {
            throw new PricingIntegrityException("Service item is not saved");
        }

        if (service.getName() == null
                || service.getName().trim().isEmpty()
                || service.getPrice() == null || service.getPrice().isNegative()
                || service.getEstimatedMinutes() < 0) {
            throw new PricingIntegrityException("Service item " + service.getId()
                    + " has invalid catalog data");
        }

        return new WorkOrderItem(
                service.getId(),
                service.getName(),
                service.getPrice(),
                service.getEstimatedMinutes()
        );
    }

    public int getServiceItemId() {
        return serviceItemId;
    }

    public String getServiceName() {
        return serviceName;
    }

    /* Price frozen when the line was created. */
    public Money getAgreedPrice() {
        return agreedPrice;
    }

    public boolean isChargeable() {
        return this.chargeable;
    }

    /* The amount the customer pays for the line */
    public Money getChargedAmount() {
        if (chargeable) {
            return this.agreedPrice;
        }
        return Money.ZERO;
    }

    public void setChargeable(boolean chargeable) {
        this.chargeable = chargeable;
    }

    /* Duration agreed at booking time. */
    public int getDurationMinutes() {
        return durationMinutes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkOrderItem that = (WorkOrderItem) o;
        return serviceItemId == that.serviceItemId
                && Objects.equals(agreedPrice, that.agreedPrice)
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
