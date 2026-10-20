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
 * <p>Ansvar: Den gemensamma basen för alla ordertyper. Varje typ skapas via fabriksmetoden i sin subklass.</p>
 */
@Entity
@Table(name = "work_order")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
public abstract class WorkOrder {

    // Steget för föreslagna starttider. Publik så att vyns tidsval använder samma steg.
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

    // Statusregeln kontrolleras först: en otillåten övergång är fel, oavsett vad ordern innehåller.
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

    // Regeln för problembeskrivning finns bara här. Typen avgör om den krävs, metoden avgör vad som räknas som tomt.
    // Statisk så att fabrikerna kan validera innan objektet skapas. Dialogen förvaliderar med samma regel.
    public static void requireProblemDescription(WorkOrderType type, String problemDescription) {
        if (type.requiresProblemDescription()
                && (problemDescription == null || problemDescription.trim().isEmpty())) {
            throw new ValidationException("error.workOrder.missingProblemDescription");
        }
    }

    protected void validateTypeSpecific() {
    } // Intentionally empty.

    // Samma krav som vid bekräftelsen. En bekräftad order som tappat t.ex. mekaniker får inte påbörjas.
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

    // En bekräftad order ska alltid vara komplett. Anropas efter redigering.
    // Ett utkast får vara ofullständigt. Vill man ta bort uppgifter återöppnas ordern som utkast först.
    public void requireCompleteIfConfirmed() {
        if (status == WorkOrderStatus.CONFIRMED) {
            requireReadyForWork();
        }
    }

    // Enda stället som avgör om ordern har det som krävs för att arbetet ska kunna genomföras.
    // Template Method: gemensamma krav här, typens egna krav i validateTypeSpecific().
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

    // Tillämpar debiteringsflaggorna från redigeringsdialogen (Template Method).
    // Typer som inte tillåter gratisrader ignorerar flaggorna, så anroparen behöver inte veta ordertypen.
    // Rader som saknas i flaggorna, t.ex. en tjänst som lades till i samma redigering, blir ej debiterbara.
    public void applyChargeFlags(Map<Integer, Boolean> chargeFlags) {
        if (!allowsNonChargeableItems()) {
            return;
        }
        for (WorkOrderItem item : items) {
            int serviceItemId = item.getServiceItemId();
            setItemChargeable(serviceItemId, chargeFlags.getOrDefault(serviceItemId, false));
        }
    }

    // Subklasser som får ha gratisrader överskuggar
    public boolean allowsNonChargeableItems() {
        return false;
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

    // Byter orderns tjänster i ett steg med samma regel som bokningen (se ServiceLines): befintliga rader behåller
    // avtalat pris, tid och debiteringsflagga, nya snapshotas och avvalda tas bort.
    // En order med tjänster får inte tömmas; ett utkast som saknar tjänster får fortsätta sakna dem.
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

    // Ordern får bara ändras innan arbetet har påbörjat
    private void ensureEditable() {
        if (!status.isEditable()) {
            throw new WorkOrderLockedException(this.getId(), this.status);
        }
    }

    // Bara ordrar som gäller en tidigare order har ett värde
    // Subklassen överskuggar metoden och null betyder “gäller ingen tidigare order”
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

    // Valfria uppgifter för utkast. Kan fyllas i senare och låses när arbetet påbörjas.
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

    // Beräknas från tjänsternas tid, sparas inte. Null utan starttid.
    public LocalTime getPlannedEndTime() {
        return plannedStartTime == null ? null : plannedSlot().getEnd();
    }

    // Krockregeln för mekaniker. Bara den andra ordern behöver reservera, så ett utkast kan varnas innan det bekräftas.
    public boolean collidesWith(WorkOrder other) {
        return plannedStartTime != null && blocks(other, plannedSlot());
    }

    // Första lediga starttid samma dag, från planerad starttid i 15-minuterssteg, som slutar före midnatt.
    // Tom om mekaniker, datum eller starttid saknas, eller om inget ryms.
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

    // Gemensamt filter för krock och förslag: en annan reserverande order för samma mekaniker och dag som överlappar luckan.
    private boolean blocks(WorkOrder other, TimeSlot slot) {
        return other.getId() != getId()
                && mechanicId != null && mechanicId.equals(other.mechanicId)
                && other.reservesOn(plannedDate)
                && slot.overlaps(other.plannedSlot());
    }

    // Om ordern reserverar sin mekaniker vid tidpunkten, t.ex. för "upptagen nu".
    public boolean reservesMechanicAt(LocalDate date, LocalTime time) {
        return reservesOn(date) && plannedSlot().contains(time);
    }

    // En reserverande order med starttid den dagen. Utan starttid finns ingen lucka att krocka med.
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

    // Skrivskyddad lista med jobben som ska utföras
    public List<WorkOrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    // Summan av de avtalade priserna, underlag för fakturan
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
