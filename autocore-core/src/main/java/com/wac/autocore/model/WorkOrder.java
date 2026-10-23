package com.wac.autocore.model;

import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.exception.IllegalStatusTransitionException;
import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.exception.WorkOrderLockedException;
import com.wac.autocore.model.value.Money;
import com.wac.autocore.model.value.TimeSlot;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * <b>WorkOrder</b>
 * <p>Responsibility: The common base for all order types. Each type is created via the factory method in its subclass.</p>
 */
@Entity
@Table(name = "work_order")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
public abstract class WorkOrder {

    // The step for suggested start times. Public so the view's time picker uses the same step.
    public static final int SUGGESTION_STEP_MINUTES = 15;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private Integer bookingId;
    private Integer vehicleId;
    private Integer mechanicId;

    @Column(name = "problem_description")
    private String problemDescription;

    @Column(name = "planned_date")
    private LocalDate plannedDate;

    @Column(name = "planned_start_time")
    private LocalTime plannedStartTime;

    @Column(name = "customer_instructions")
    private String customerInstructions;

    @Column(name = "comments")
    private String comments;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "work_order_service_item", joinColumns = @JoinColumn(name = "work_order_id"))
    private List<WorkOrderItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private WorkOrderStatus status = WorkOrderStatus.DRAFT;

    protected WorkOrder() {
    }

    protected WorkOrder(Integer bookingId, Integer vehicleId, Integer mechanicId, List<WorkOrderItem> items) {
        this.bookingId = bookingId;
        this.vehicleId = vehicleId;
        this.mechanicId = mechanicId;
        this.items.addAll(items);
    }

    // The status rule is checked first: a disallowed transition is an error, regardless of what the order contains.
    private void requireCanChangeTo(WorkOrderStatus next) {
        if (!this.status.canChangeTo(next)) {
            throw new IllegalStatusTransitionException(this.status, next);
        }
    }

    private void transitionTo(WorkOrderStatus next) {
        requireCanChangeTo(next);
        this.status = next;
    }

    public void confirm() {
        requireCanChangeTo(WorkOrderStatus.CONFIRMED);
        requireReadyForWork();
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
        requireProblemDescription(getType(), problemDescription);
        if (plannedStartTime != null) {
            plannedSlot().requireWithinDay();
        }
    }

    // The rule for the problem description exists only here. The type decides whether it is required, the method decides what counts as empty.
    // Static so the factories can validate before the object is created. The dialog pre-validates with the same rule.
    public static void requireProblemDescription(WorkOrderType type, String problemDescription) {
        if (type.requiresProblemDescription()
                && (problemDescription == null || problemDescription.trim().isEmpty())) {
            throw new ValidationException("error.workOrder.missingProblemDescription");
        }
    }

    protected void validateTypeSpecific() {
    } // Intentionally empty.

    // Same requirements as on confirmation. A confirmed order that has lost e.g. its mechanic may not be started.
    public void start() {
        requireCanChangeTo(WorkOrderStatus.IN_PROGRESS);
        requireReadyForWork();
        transitionTo(WorkOrderStatus.IN_PROGRESS);
    }

    public void complete() {
        transitionTo(WorkOrderStatus.COMPLETED);
    }

    public void cancel() {
        transitionTo(WorkOrderStatus.CANCELLED);
    }

    public void reopen() {
        transitionTo(WorkOrderStatus.DRAFT);
    }

    // A confirmed order must always be complete. Called after editing.
    // A draft may be incomplete. To remove details, the order is first reopened as a draft.
    public void requireCompleteIfConfirmed() {
        if (status == WorkOrderStatus.CONFIRMED) {
            requireReadyForWork();
        }
    }

    // The only place that decides whether the order has what is required for the work to be carried out.
    // Template Method: common requirements here, the type's own requirements in validateTypeSpecific().
    private void requireReadyForWork() {
        validateCommon();
        validateTypeSpecific();
    }

    public void setItemChargeable(int serviceItemId, boolean chargeable) {
        ensureEditable();
        if (!chargeable && !allowsNonChargeableItems()) {
            throw new ValidationException("error.workOrder.nonChargeableNotAllowed");
        }
        for (WorkOrderItem item : items) {
            if (item.getServiceItemId() == serviceItemId) {
                item.setChargeable(chargeable);
                return;
            }
        }
        throw new EntityNotFoundException("WorkOrderItem", serviceItemId);
    }

    // Applies the charge flags from the edit dialog (Template Method).
    // Types that do not allow free lines ignore the flags, so the caller does not need to know the order type.
    // Lines missing from the flags, e.g. a service added in the same edit, become non-chargeable.
    public void applyChargeFlags(Map<Integer, Boolean> chargeFlags) {
        if (!allowsNonChargeableItems()) {
            return;
        }
        for (WorkOrderItem item : items) {
            int serviceItemId = item.getServiceItemId();
            setItemChargeable(serviceItemId, chargeFlags.getOrDefault(serviceItemId, false));
        }
    }

    // Subclasses that are allowed to have free lines override this
    public boolean allowsNonChargeableItems() {
        return false;
    }

    // Adds a job to the order. Only subclasses that allow new jobs expose this.
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

    // Replaces the order's services in one step with the same rule as the booking (see ServiceLines): existing lines keep
    // agreed price, duration and charge flag, new ones are snapshotted and deselected ones are removed.
    // An order with services may not be emptied; a draft that has no services may continue to have none.
    public void replaceItems(List<ServiceItem> services) {
        ensureEditable();
        List<WorkOrderItem> next = ServiceLines.merge(items, services,
                WorkOrderItem::getServiceItemId, WorkOrderItem::snapshotOf);
        if (next.isEmpty() && !items.isEmpty()) {
            throw new ValidationException("error.workOrder.missingServices");
        }
        items.clear();
        items.addAll(next);
    }

    // The order may only be changed before the work has started
    private void ensureEditable() {
        if (!status.isEditable()) {
            throw new WorkOrderLockedException(this.getId(), this.status);
        }
    }

    // Only orders that relate to a previous order have a value
    // The subclass overrides the method and null means "does not relate to any previous order"
    public Integer getOriginalWorkOrderId() {
        return null;
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
        ensureEditable();
        this.mechanicId = mechanicId;
    }

    public String getProblemDescription() {
        return problemDescription;
    }

    protected void setProblemDescription(String problemDescription) {
        this.problemDescription = problemDescription;
    }

    // Optional details for drafts. Can be filled in later and are locked when the work starts.
    public LocalDate getPlannedDate() {
        return plannedDate;
    }

    public void setPlannedDate(LocalDate plannedDate) {
        ensureEditable();
        this.plannedDate = plannedDate;
    }

    public LocalTime getPlannedStartTime() {
        return plannedStartTime;
    }

    public void setPlannedStartTime(LocalTime plannedStartTime) {
        ensureEditable();
        this.plannedStartTime = plannedStartTime;
    }

    // Calculated from the services' durations, not stored. Null without a start time.
    public LocalTime getPlannedEndTime() {
        return plannedStartTime == null ? null : plannedSlot().getEnd();
    }

    // The clash rule for mechanics. Only the other order needs to reserve, so a draft can be warned before it is confirmed.
    public boolean collidesWith(WorkOrder other) {
        return plannedStartTime != null && blocks(other, plannedSlot());
    }

    // First available start time the same day, from the planned start time in 15-minute steps, that ends before midnight.
    // Empty if mechanic, date or start time is missing, or if nothing fits.
    public Optional<LocalTime> nextFreeStartTime(List<WorkOrder> others) {
        if (mechanicId == null || plannedDate == null || plannedStartTime == null) {
            return Optional.empty();
        }
        int duration = getTotalDurationMinutes();
        for (LocalTime candidate = plannedStartTime; ; candidate = candidate.plusMinutes(SUGGESTION_STEP_MINUTES)) {
            TimeSlot slot = TimeSlot.of(candidate, duration);
            if (!slot.endsWithinDay() || candidate.isBefore(plannedStartTime)) {
                return Optional.empty();
            }
            if (isFree(slot, others)) {
                return Optional.of(candidate);
            }
        }
    }

    private boolean isFree(TimeSlot slot, List<WorkOrder> others) {
        for (WorkOrder other : others) {
            if (blocks(other, slot)) {
                return false;
            }
        }
        return true;
    }

    // Common filter for clashes and suggestions: another reserving order for the same mechanic and day that overlaps the slot.
    private boolean blocks(WorkOrder other, TimeSlot slot) {
        return other.getId() != getId()
                && mechanicId != null && mechanicId.equals(other.mechanicId)
                && other.reservesOn(plannedDate)
                && slot.overlaps(other.plannedSlot());
    }

    // Whether the order reserves its mechanic at the given time, e.g. for "busy now".
    public boolean reservesMechanicAt(LocalDate date, LocalTime time) {
        return reservesOn(date) && plannedSlot().contains(time);
    }

    // A reserving order with a start time that day. Without a start time there is no slot to clash with.
    private boolean reservesOn(LocalDate date) {
        return status.reservesMechanic()
                && plannedStartTime != null
                && date != null && date.equals(plannedDate);
    }

    private TimeSlot plannedSlot() {
        return TimeSlot.of(plannedStartTime, getTotalDurationMinutes());
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

    // Read-only list of the jobs to be performed
    public List<WorkOrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    // The sum of the agreed prices, the basis for the invoice
    public Money getTotalPrice() {
        return items.stream()
                .map(WorkOrderItem::getChargedAmount)
                .reduce(Money.ZERO, Money::add);
    }

    public int getTotalDurationMinutes() {
        return items.stream()
                .mapToInt(WorkOrderItem::getDurationMinutes)
                .sum();
    }

    public WorkOrderStatus getStatus() {
        return status;
    }

    // Each subclass specifies its type, so new order types do not require changes in the view
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
