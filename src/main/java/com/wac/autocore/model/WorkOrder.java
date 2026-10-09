package com.wac.autocore.model;

import com.wac.autocore.exception.IllegalStatusTransitionException;
import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.exception.WorkOrderLockedException;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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

    @Column (name = "problem_description")
    private String problemDescription;

    @Column(name = "planned_date")
    private LocalDate plannedDate;

    @Column(name = "customer_instructions")
    private String customerInstructions;

    @Column(name = "comments")
    private String comments;

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
    public void reopen()   { transitionTo(WorkOrderStatus.DRAFT); }

    public void setItemChargeable(int serviceItemId, boolean chargeable) {
        ensureEditable();
        for (WorkOrderItem item : items) {
            if (item.getServiceItemId() == serviceItemId) {
                item.setChargeable(chargeable);
                return;
            }
        }
        throw new EntityNotFoundException("WorkOrderItem", serviceItemId);
    }

    // Lägger till ett jobb på ordern. Bara subklasser som tillåter nya jobb exponerar detta.
    protected void addItem(WorkOrderItem item) {
        Objects.requireNonNull(item, "item must not be null");
        ensureEditable();

        boolean alreadyAdded = items.stream()
                .anyMatch(existing -> existing.getServiceItemId() == item.getServiceItemId());
        if (alreadyAdded) {
            throw new ValidationException("error.workOrder.duplicateService", item.getServiceName());
        }

        items.add(item);
    }

    /// Overloaded method to add a ServiceItem instead of a WorkOrderItem.
    public void addItem(ServiceItem service) {
        addItem(WorkOrderItem.snapshotOf(service));
    }

    public void removeItem(int serviceItemId) {
        ensureEditable();

        boolean itemExists = items.stream()
                .anyMatch(item -> item.getServiceItemId() == serviceItemId);

        if (!itemExists) {
            throw new EntityNotFoundException("WorkOrderItem", serviceItemId);
        }

        if (items.size() == 1) {
            throw new ValidationException("error.workOrder.missingServices");
        }

        items.removeIf(item -> item.getServiceItemId() == serviceItemId);
    }

    // Ordern får bara ändras innan arbetet har påbörjat
    private void ensureEditable() {
        if (status != WorkOrderStatus.DRAFT && status != WorkOrderStatus.CONFIRMED) {
            throw new WorkOrderLockedException(this.getId(), this.status);
        }
    }

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

    public String getProblemDescription() {
        return problemDescription;
    }

    protected void setProblemDescription(String problemDescription) {
        this.problemDescription = problemDescription;
    }

    // Valfria uppgifter för utkast. Kan fyllas i senare och låses när arbetet påbörjas.
    public LocalDate getPlannedDate() {
        return plannedDate;
    }

    public void setPlannedDate(LocalDate plannedDate) {
        ensureEditable();
        this.plannedDate = plannedDate;
    }

    public String getCustomerInstructions() {
        return customerInstructions;
    }

    public void setCustomerInstructions(String customerInstructions) {
        ensureEditable();
        this.customerInstructions = customerInstructions;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        ensureEditable();
        this.comments = comments;
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

    // Varje subklass anger sin typ, så nya ordertyper inte kräver ändringar i vyn
    public abstract WorkOrderType getType();

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