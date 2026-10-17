package com.wac.autocore.service;

import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.exception.MechanicDoubleBookingException;
import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.*;
import com.wac.autocore.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <b>WorkOrderService</b>
 * <p>Ansvar: Affärslogik för arbetsordrar: Skapa, starta och avsluta, samt hämtning för vyerna.</p>
 * <p>{@link #createWorkOrder(int)} kastar ett {@code DomainException} med språknyckel när
 * ordern inte kan skapas, så att vyn kan visa rätt orsak via {@code ErrorFacade}.
 * {@link #startWorkOrder(int)} och {@link #completeWorkOrder(int)} kastar {@code IllegalStatusTransitionException}
 * när övergången inte är tillåten.</p>
 */
@Validated
@Service
@Transactional
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final BookingRepository bookingRepository;
    private final MechanicRepository mechanicRepository;
    private final VehicleRepo vehicleRepo;
    private final ServiceItemRepository serviceItemRepository;
    private final BookingService bookingService;

    public WorkOrderService(WorkOrderRepository workOrderRepository,
                            BookingRepository bookingRepository,
                            MechanicRepository mechanicRepository,
                            VehicleRepo vehicleRepo,
                            ServiceItemRepository serviceItemRepository,
                            BookingService bookingService) {
        this.workOrderRepository = workOrderRepository;
        this.bookingRepository = bookingRepository;
        this.mechanicRepository = mechanicRepository;
        this.vehicleRepo = vehicleRepo;
        this.serviceItemRepository = serviceItemRepository;
        this.bookingService = bookingService;
    }

    @Transactional(readOnly = true)
    public List<WorkOrder> findAll() {
        return workOrderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<WorkOrder> findById(int id) {
        return workOrderRepository.findById(id);
    }

    private WorkOrder require(int id) {
        return workOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("WorkOrder", id));
    }

    @Transactional(readOnly = true)
    public List<WorkOrder> findByMechanicId(int mechanicId) {
        return workOrderRepository.findByMechanicId(mechanicId);
    }

    @Transactional(readOnly = true)
    public List<WorkOrder> findCompletedNotInvoiced() {
        return workOrderRepository.findCompletedNotInvoiced();
    }

    // Hämtar alla arbetsordrar med status "COMPLETED" för att kunna skapa en garantiorder.
    @Transactional(readOnly = true)
    public List<WorkOrder> findCompletedWorkOrders() {
        return workOrderRepository.findByStatus(WorkOrderStatus.COMPLETED);
    }

    @Transactional(readOnly = true)
    public List<Booking> findBookableBookings() {
        return bookingRepository.findBookable(BookingStatus.BOOKED, WorkOrderStatus.CANCELLED);
    }

    // Skapar en planerad arbetsorder för en bokning och sparar den som utkast.
    // Bokningens föreslagna mekaniker, tid och tjänster, med avtalade priser, ärvs från bokningen.
    // Krockar mekanikern sparas utkastet ändå och resultatet innehåller varningen och nästa lediga tid.
    public WorkOrderScheduleResult createWorkOrder(@Positive(message = "error.notFound") int bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking", bookingId));

        if (workOrderRepository.existsByBookingIdAndStatusNot(bookingId, WorkOrderStatus.CANCELLED)) {
            throw new ValidationException("error.workOrder.alreadyExists", String.valueOf(bookingId));
        }

        WorkOrder workOrder = plannedWorkOrderFor(booking);
        return scheduleResult(workOrderRepository.save(workOrder));
    }

    // Gemensamt för createWorkOrder och reopenWorkOrder: bokningen måste vara ledig och fordonet finnas.
    // Ordern skapas från bokningen som den ser ut nu, och bokningen låses.
    // Den föreslagna mekanikern slås inte upp: främmande nyckeln garanterar att den finns.
    private WorkOrder plannedWorkOrderFor(Booking booking) {
        if (booking.getStatus() != BookingStatus.BOOKED) {
            throw new ValidationException("error.workOrder.bookingNotBookable", String.valueOf(booking.getId()));
        }

        if (!vehicleRepo.existsById(booking.getVehicleId())) {
            throw new EntityNotFoundException("Vehicle", booking.getVehicleId());
        }

        WorkOrder workOrder = PlannedWorkOrder.createFrom(booking);

        syncBooking(workOrder);
        return workOrder;
    }

    // Skapar en garantiorder från en tidigare arbetsorder och sparar den som utkast.
    public WarrantyWorkOrder createWarranty(@Positive int originalWorkOrderId,
                                            String problemDescription) {
        WorkOrder original = require(originalWorkOrderId);

        WarrantyWorkOrder warranty =
                WarrantyWorkOrder.draft(original, problemDescription);

        return workOrderRepository.save(warranty);
    }

    // Skapar en drop-in-order (bil utan bokning) och sparar den som utkast.
    // Servicen kontrollerar att fordonet finns, modellen validerar resten.
    // Tjänster är valfria för ett utkast; null behandlas som "inga tjänster".
    public DropInWorkOrder createDropIn(@Positive(message = "error.notFound") int vehicleId,
                                        String problemDescription,
                                        List<Integer> serviceItemIds) {
        if (!vehicleRepo.existsById(vehicleId)) {
            throw new EntityNotFoundException("Vehicle", vehicleId);
        }

        // Tjänsterna kopieras som snapshots så senare prisändringar inte påverkar ordern.
        DropInWorkOrder dropIn = DropInWorkOrder.draft(vehicleId, problemDescription);
        dropIn.replaceItems(serviceItemRepository.requireAllById(serviceItemIds));
        return workOrderRepository.save(dropIn);
    }

    public void startWorkOrder(@Positive int id) {
        WorkOrder workOrder = require(id);
        workOrder.start();
        syncBooking(workOrder);
        workOrderRepository.save(workOrder);
    }

    public void completeWorkOrder(@Positive int id) {
        WorkOrder workOrder = require(id);
        workOrder.complete();
        syncBooking(workOrder);
        workOrderRepository.save(workOrder);
    }

    // Bokningens status följer arbetsordern. Hämtning, regel och sparning ligger i BookingService,
    // samma väg som när planen följer ordern (followBooking).
    // En arbetsorder utan bokning (drop-in, reklamation) har ingen bokning att uppdatera.
    private void syncBooking(WorkOrder workOrder) {
        Integer bookingId = workOrder.getBookingId();
        if (bookingId == null) {
            return;
        }
        bookingService.followWorkOrderStatus(bookingId, workOrder.getStatus());
    }

    // Bekräftelsen reserverar mekanikern, så en krock stoppar den och inget sparas.
    public void confirmWorkOrder(@Positive int id) {
        WorkOrder workOrder = require(id);
        workOrder.confirm();
        rejectConflictsIfReserved(scheduleResult(workOrder));
        syncBooking(workOrder);
        workOrderRepository.save(workOrder);
    }

    // En avbruten planerad order öppnas som en ny order från bokningen som den ser ut nu, eftersom bokningen
    // kan ha ändrats medan ordern var avbruten. Den avbrutna ordern ligger kvar som historik. Kommentarer och
    // kundinstruktioner följer med, och resultatet listar vad som skiljer mot den avbrutna ordern.
    // Övriga ordrar återöppnas som utkast (samma order). Bokningen förblir då låst: DRAFT mappar till WORK_ORDER_CREATED.
    public WorkOrderReopenResult reopenWorkOrder(@Positive int id) {
        WorkOrder workOrder = require(id);

        if (workOrder.getType() == WorkOrderType.PLANNED && workOrder.getStatus() == WorkOrderStatus.CANCELLED) {
            return recreateFromBooking(workOrder);
        }

        workOrder.reopen();
        syncBooking(workOrder);
        workOrderRepository.save(workOrder);
        return new WorkOrderReopenResult(WorkOrderScheduleResult.withoutConflicts(workOrder),
                Collections.<WorkOrderChange>emptyList());
    }

    private WorkOrderReopenResult recreateFromBooking(WorkOrder cancelled) {
        int bookingId = cancelled.getBookingId();
        if (workOrderRepository.existsByBookingIdAndStatusNot(bookingId, WorkOrderStatus.CANCELLED)) {
            throw new ValidationException("error.workOrder.bookingHasActiveOrder", String.valueOf(bookingId));
        }
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking", bookingId));

        WorkOrder reopened = plannedWorkOrderFor(booking);
        reopened.setComments(cancelled.getComments());
        reopened.setCustomerInstructions(cancelled.getCustomerInstructions());

        WorkOrder saved = workOrderRepository.save(reopened);
        return new WorkOrderReopenResult(scheduleResult(saved), WorkOrderChange.between(cancelled, saved));
    }

    // Avbryter ordern och släpper bokningen (BOOKED), så den kan få en ny order, t.ex med annan mekaniker.
    public void cancelWorkOrder(@Positive int id) {
        WorkOrder workOrder = require(id);
        workOrder.cancel();
        syncBooking(workOrder);
        workOrderRepository.save(workOrder);
    }

    // Mekaniker är valfri. Ett utkast som krockar sparas med varning; en bekräftad order som krockar stoppas.
    public WorkOrderScheduleResult updateWorkOrder(@Positive int id,
                                                   List<@NotNull @Positive Integer> serviceItemIds,
                                                   @Positive Integer mechanicId,
                                                   LocalDate plannedDate,
                                                   LocalTime plannedStartTime,
                                                   String customerInstructions,
                                                   String comments,
                                                   Map<Integer, Boolean> chargeFlags) {
        WorkOrder workOrder = require(id);

        if (mechanicId != null) {
            requireMechanic(mechanicId);
        }

        workOrder.replaceItems(serviceItemRepository.requireAllById(serviceItemIds));

        workOrder.applyChargeFlags(chargeFlags);

        workOrder.setMechanicId(mechanicId);
        workOrder.setPlannedDate(plannedDate);
        workOrder.setPlannedStartTime(plannedStartTime);
        workOrder.setCustomerInstructions(customerInstructions);
        workOrder.setComments(comments);

        WorkOrderScheduleResult result = scheduleResult(workOrder);
        rejectConflictsIfReserved(result);
        followBooking(workOrder);
        workOrderRepository.save(workOrder);
        return result;
    }

    // Bokningen följer orderns datum, starttid och tjänster så att kalendern och kontrollen av fordonet stämmer.
    // Mekanikern följer inte med: ordern äger den, och bokningens förslag är kundens önskemål.
    // Fordonskontrollen och laddningen av tjänster finns i BookingService och dupliceras inte här.
    private void followBooking(WorkOrder workOrder) {
        Integer bookingId = workOrder.getBookingId();
        if (bookingId == null) {
            return;
        }
        List<Integer> serviceItemIds = workOrder.getItems().stream()
                .map(WorkOrderItem::getServiceItemId)
                .collect(Collectors.toList());
        bookingService.followWorkOrder(bookingId, workOrder.getPlannedDate(), workOrder.getPlannedStartTime(),
                serviceItemIds);
    }

    private void requireMechanic(int mechanicId) {
        mechanicRepository.findById(mechanicId)
                .orElseThrow(() -> new EntityNotFoundException("Mechanic", mechanicId));
    }

    // Enda platsen som hämtar krockar för mekanikern. Regeln och förslaget ligger i WorkOrder.
    // Utan mekaniker eller datum finns inget att krocka med och databasen frågas inte.
    private WorkOrderScheduleResult scheduleResult(WorkOrder workOrder) {
        if (workOrder.getMechanicId() == null || workOrder.getPlannedDate() == null) {
            return WorkOrderScheduleResult.withoutConflicts(workOrder);
        }
        List<WorkOrder> sameDay = workOrderRepository
                .findByMechanicIdAndPlannedDate(workOrder.getMechanicId(), workOrder.getPlannedDate());
        List<WorkOrder> conflicts = sameDay.stream()
                .filter(workOrder::collidesWith)
                .collect(Collectors.toList());
        if (conflicts.isEmpty()) {
            return WorkOrderScheduleResult.withoutConflicts(workOrder);
        }
        return new WorkOrderScheduleResult(workOrder, conflicts, workOrder.nextFreeStartTime(sameDay));
    }

    // En order som reserverar mekanikern (bekräftad eller pågående) får inte krocka. Ett utkast får det, med varning.
    private void rejectConflictsIfReserved(WorkOrderScheduleResult result) {
        WorkOrder workOrder = result.getWorkOrder();
        if (workOrder.getStatus().reservesMechanic() && !result.getMechanicConflicts().isEmpty()) {
            throw new MechanicDoubleBookingException(mechanicName(workOrder.getMechanicId()),
                    workOrder.getPlannedDate(), workOrder.getPlannedStartTime(), workOrder.getPlannedEndTime());
        }
    }

    // Namnet behövs bara i felmeddelandet. Faller tillbaka på id:t om mekanikern inte går att läsa.
    private String mechanicName(int mechanicId) {
        return mechanicRepository.findById(mechanicId)
                .map(Mechanic::getName)
                .orElse(String.valueOf(mechanicId));
    }
}
