package com.wac.autocore.model;

import com.wac.autocore.exception.IllegalStatusTransitionException;
import com.wac.autocore.exception.ValidationException;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <b>WorkOrder</b>
 * <p>Ansvar: Den gemensamma basen för alla ordertyper. Varje typ skapas via fabriksmetoden i sin subklass.</p>
 */
@Entity
@Table(name = "work_order")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
public abstract class WorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private Integer bookingId;
    private Integer vehicleId;
    private Integer mechanicId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "work_order_service_item", joinColumns = @JoinColumn(name = "work_order_id"))
    @Column(name = "service_item_id")
    private List<WorkOrderItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private WorkOrderStatus status = WorkOrderStatus.DRAFT;

    protected WorkOrder() {}

    protected WorkOrder(Integer bookingId, Integer vehicleId, Integer mechanicId, List<WorkOrderItem> items) {
        this.bookingId = bookingId;
        this.vehicleId = vehicleId;
        this.mechanicId = mechanicId;
        this.items.addAll(items);
    }

    private void transitionTo(WorkOrderStatus next) {
        if (!this.status.canChangeTo(next)) {
            throw new IllegalStatusTransitionException(this.status, next);
        }
        this.status = next;
    }

    public void confirm()  {
        validateCommon();
        validateTypeSpecific();
        transitionTo(WorkOrderStatus.CONFIRMED);
    }

    private void validateCommon() {
        if (vehicleId == null) {
            throw new ValidationException("error.workOrder.missingVehicle");
        }
        if (items.isEmpty()) {
            throw new ValidationException("error.workOrder.missingServices");
        }
        if (mechanicId == null) {
            throw new ValidationException("error.workOrder.missingMechanic");
        }
    }

    protected void validateTypeSpecific() {} // Intentionally empty.
    public void start()    { transitionTo(WorkOrderStatus.IN_PROGRESS); }
    public void complete() { transitionTo(WorkOrderStatus.COMPLETED); }
    public void cancel()   { transitionTo(WorkOrderStatus.CANCELLED); }

    public int getId() {
        return id;
    }

    public Integer getBookingId() {
        return bookingId;
    }

    public Integer getVehicleId() {
        return vehicleId;
    }

    public Integer getMechanicId() {
        return mechanicId;
    }

    public void setMechanicId(Integer mechanicId) {
        this.mechanicId = mechanicId;
    }

    // Skrivskyddad lista med jobben som ska utföras
    public List<WorkOrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    // Summan av de avtalade priserna, underlag för fakturan
    public double getTotalPrice() {
        return items.stream()
                .mapToDouble(WorkOrderItem::getAgreedPrice)
                .sum();
    }

    public int getTotalDurationMinutes() {
        return items.stream()
                .mapToInt(WorkOrderItem::getDurationMinutes)
                .sum();
    }

    public WorkOrderStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return id +
                " - Booking ID: " + bookingId +
                " | Vehicle ID: " + vehicleId +
                " | Mechanic ID: " + mechanicId +
                " | Jobs: " + items.size() +
                " | Status: " + status;
    }
}