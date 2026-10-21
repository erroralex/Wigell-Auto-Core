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
 * <p>Ansvar: En bokning av ett fordon hos en mekaniker. Bokningen äger sina tjänsterader
 * ({@link BookingServiceItem}) där namn, pris och tid frystes när tjänsten lades till.</p>
 * <p>Bokningen ändras bara via konstruktorn och {@link #update}. Båda kräver datum, starttid
 * och minst en tjänst, behåller avtalade priser för befintliga rader, snapshotar bara nya
 * tjänster och räknar sluttiden från radernas frysta minuter. En bokning får inte sluta vid
 * eller efter midnatt, och {@code update} och {@link #cancel} kastar {@link BookingLockedException} när
 * bokningen har en arbetsorder eller redan är avbokad.</p>
 * <p>Medan arbetsordern planeras ({@link BookingStatus#WORK_ORDER_CREATED}) följer bokningen orderns datum,
 * starttid och tjänster via {@link #applyWorkOrderPlan}, med samma regler.</p>
 */
@Entity
@Table(name = "booking")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "vehicle_id", nullable = false)
    private int vehicleId;

    // Föreslagen mekaniker, valfri. Reserverar ingen tid; mekanikern reserveras först på arbetsordern.
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


     // Ändrar hela bokningen i ett steg.
     // Befintliga tjänster behåller sitt avtalade pris och sin tid,
     // nya tjänster snapshotas till dagens pris och avvalda tjänster tas bort.
     // Om något är ogiltigt kastas ett undantag och bokningen lämnas oförändrad.
    public void update(int vehicleId, Integer mechanicId, LocalDate date,
                       LocalTime startTime, String description, List<ServiceItem> services) {
        ensureEditable();
        apply(vehicleId, mechanicId, date, startTime, description, services);
    }

    /**
     * Låter bokningen följa sin arbetsorder: datum, starttid och tjänster sätts från ordern. Fordon, föreslagen
     * mekaniker och beskrivning behålls. Samma regler som {@link #update}: minst en tjänst, behållna avtalade
     * priser, snapshot av nya tjänster, sluttid från snapshots och inte över midnatt.
     *
     * @throws BookingLockedException om bokningen inte har en arbetsorder som fortfarande planeras
     */
    public void applyWorkOrderPlan(LocalDate date, LocalTime startTime, List<ServiceItem> services) {
        if (status != BookingStatus.WORK_ORDER_CREATED) {
            throw new BookingLockedException(id, status);
        }
        apply(vehicleId, mechanicId, date, startTime, description, services);
    }

    /**
     * Skapar en ny bokning med samma fordon, föreslagna mekaniker, beskrivning och tjänster, men med nytt
     * datum och ny starttid. Tjänsterna hämtas från {@code currentCatalog}, så kopian får dagens
     * pris och tid. Kopian byggs via konstruktorn och följer därför samma regler som en ny bokning.
     *
     * @throws EntityNotFoundException om en av bokningens tjänster saknas i katalogen
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

    // Validerar allt och bygger den nya radlistan innan något fält ändras,
    // så att ett undantag aldrig lämnar bokningen halvt ändrad.
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

        // Samma listinstans behålls: Hibernate kräver det för samlingar med orphanRemoval.
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

    // Avbokning tillåts bara innan en arbetsorder finns. CANCELLED är ett slutläge;
    // att ångra görs genom att skapa en ny bokning från den avbokade (copyAsNew).
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
