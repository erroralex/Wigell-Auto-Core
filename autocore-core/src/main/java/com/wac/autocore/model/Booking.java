package com.wac.autocore.model;

import com.wac.autocore.exception.BookingLockedException;
import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.value.Money;
import com.wac.autocore.model.value.TimeSlot;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <b>Booking</b>
 * <p>Responsibility: A booking of a vehicle with a mechanic. The booking owns its service lines
 * ({@link BookingServiceItem}) where name, price and duration were frozen when the service was added.</p>
 * <p>The booking is changed only via the constructor and {@link #update}. Both require date, start time
 * and at least one service, keep agreed prices for existing lines, snapshot only new
 * services and calculate the end time from the lines' frozen minutes. A booking may not end at
 * or after midnight, and {@code update} and {@link #cancel} throw {@link BookingLockedException} when
 * the booking has a work order or is already cancelled.</p>
 * <p>While the work order is being planned ({@link BookingStatus#WORK_ORDER_CREATED}) the booking follows the order's date,
 * start time and services via {@link #applyWorkOrderPlan}, with the same rules.</p>
 */
@Entity
@Table(name = "booking")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "vehicle_id", nullable = false)
    private int vehicleId;

    // Suggested mechanic, optional. Reserves no time; the mechanic is reserved only on the work order.
    @Column(name = "mechanic_id")
    private Integer mechanicId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private BookingStatus status = BookingStatus.BOOKED;

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

    public Booking(int vehicleId, Integer mechanicId, LocalDate date,
                   LocalTime startTime, String description, List<ServiceItem> services) {
        apply(vehicleId, mechanicId, date, startTime, description, services);
    }


     // Changes the whole booking in one step.
     // Existing services keep their agreed price and duration,
     // new services are snapshotted at today's price and deselected services are removed.
     // If anything is invalid an exception is thrown and the booking is left unchanged.
    public void update(int vehicleId, Integer mechanicId, LocalDate date,
                       LocalTime startTime, String description, List<ServiceItem> services) {
        ensureEditable();
        apply(vehicleId, mechanicId, date, startTime, description, services);
    }

    /**
     * Lets the booking follow its work order: date, start time and services are set from the order. Vehicle, suggested
     * mechanic and description are kept. Same rules as {@link #update}: at least one service, kept agreed
     * prices, snapshot of new services, end time from snapshots and not past midnight.
     *
     * @throws BookingLockedException if the booking does not have a work order that is still being planned
     */
    public void applyWorkOrderPlan(LocalDate date, LocalTime startTime, List<ServiceItem> services) {
        if (status != BookingStatus.WORK_ORDER_CREATED) {
            throw new BookingLockedException(id, status);
        }
        apply(vehicleId, mechanicId, date, startTime, description, services);
    }

    /**
     * Creates a new booking with the same vehicle, suggested mechanic, description and services, but with a new
     * date and a new start time. The services are fetched from {@code currentCatalog}, so the copy gets today's
     * price and duration. The copy is built via the constructor and therefore follows the same rules as a new booking.
     *
     * @throws EntityNotFoundException if one of the booking's services is missing from the catalog
     */
    public Booking copyAsNew(LocalDate date, LocalTime startTime, List<ServiceItem> currentCatalog) {
        List<ServiceItem> services = new ArrayList<>();
        for (BookingServiceItem item : items) {
            services.add(findInCatalog(currentCatalog, item.getServiceItemId()));
        }
        return new Booking(vehicleId, mechanicId, date, startTime, description, services);
    }

    private static ServiceItem findInCatalog(List<ServiceItem> catalog, int serviceItemId) {
        for (ServiceItem service : catalog) {
            if (service.getId() == serviceItemId) {
                return service;
            }
        }
        throw new EntityNotFoundException("ServiceItem", serviceItemId, "error.serviceNotFound");
    }

    // Validates everything and builds the new line list before any field is changed,
    // so an exception never leaves the booking half changed.
    private void apply(int vehicleId, Integer mechanicId, LocalDate date,
                       LocalTime startTime, String description, List<ServiceItem> services) {
        if (date == null) {
            throw new ValidationException("error.dateSelect");
        }
        if (startTime == null) {
            throw new ValidationException("error.startTimeSelect");
        }
        if (services == null || services.isEmpty()) {
            throw new ValidationException("error.serviceSelect");
        }

        List<BookingServiceItem> nextItems = itemsFor(services);
        LocalTime newEndTime = TimeSlot.of(startTime, durationOf(nextItems)).requireWithinDay().getEnd();

        this.vehicleId = vehicleId;
        this.mechanicId = mechanicId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = newEndTime;
        this.description = description;

        // The same list instance is kept: Hibernate requires it for collections with orphanRemoval.
        items.clear();
        for (BookingServiceItem item : nextItems) {
            item.setBooking(this);
            items.add(item);
        }
    }

    private List<BookingServiceItem> itemsFor(List<ServiceItem> services) {
        return ServiceLines.merge(items, services, BookingServiceItem::getServiceItemId, BookingServiceItem::snapshotOf);
    }

    private static int durationOf(List<BookingServiceItem> bookingItems) {
        int minutes = 0;
        for (BookingServiceItem item : bookingItems) {
            minutes += item.getDurationMinutes();
        }
        return minutes;
    }

    // Cancellation is only allowed before a work order exists. CANCELLED is a final state;
    // undoing is done by creating a new booking from the cancelled one (copyAsNew).
    public void cancel() {
        ensureEditable();
        status = BookingStatus.CANCELLED;
    }

    private void ensureEditable() {
        if (!status.isEditable()) {
            throw new BookingLockedException(id, status);
        }
    }

    public int getId() {
        return id;
    }

    public int getVehicleId() {
        return vehicleId;
    }

    public Integer getMechanicId() {
        return mechanicId;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public String getDescription() {
        return description;
    }

    public List<BookingServiceItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Money getTotalAgreedPrice() {
        return items.stream()
                .map(BookingServiceItem::getPriceAtBooking)
                .reduce(Money.ZERO, Money::add);
    }

    public int getTotalDurationMinutes() {
        return durationOf(items);
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void followWorkOrder(WorkOrderStatus workOrderStatus) {
        if (status == BookingStatus.CANCELLED) {
            throw new BookingLockedException(id, status);
        }
        status =  BookingStatus.forWorkOrder(workOrderStatus);
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
