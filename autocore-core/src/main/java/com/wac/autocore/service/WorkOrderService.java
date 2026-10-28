package com.wac.autocore.service;

import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.exception.MechanicDoubleBookingException;
import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.logging.LogKeys;
import com.wac.autocore.model.Booking;
import com.wac.autocore.model.BookingStatus;
import com.wac.autocore.model.DropInWorkOrder;
import com.wac.autocore.model.PlannedWorkOrder;
import com.wac.autocore.model.WarrantyWorkOrder;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderChange;
import com.wac.autocore.model.WorkOrderItem;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.model.WorkOrderType;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.MechanicRepository;
import com.wac.autocore.repository.ServiceItemRepository;
import com.wac.autocore.repository.VehicleRepo;
import com.wac.autocore.repository.WorkOrderRepository;
import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <b>WorkOrderService</b>
 * <p>Responsibility: Business logic for work orders: create, start and complete, plus fetching for the views.</p>
 * <p>{@link #createWorkOrder(int)} throws a {@code DomainException} with a language key when
 * the order cannot be created, so the view can show the correct reason via {@code ErrorFacade}.
 * {@link #startWorkOrder(int)} and {@link #completeWorkOrder(int)} throw {@code IllegalStatusTransitionException}
 * when the transition is not allowed.</p>
 *
 * <p>Implements {@link WorkOrderLifecycle} and is the <b>real subject</b> in the
 * <b>Proxy</b> pattern (GoF). The standalone program puts a protection proxy in front of it.</p>
 */
@Validated
@Service
@Transactional
@PreAuthorize("@access.can('DATA_READ')") // Ensures that only users with 'DATA_READ' permission can access the service methods.
public class WorkOrderService implements WorkOrderLifecycle {

    private static final Logger log = LoggerFactory.getLogger(WorkOrderService.class);

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

    // Fetches all work orders with status "COMPLETED" so a warranty order can be created.
    @Transactional(readOnly = true)
    public List<WorkOrder> findCompletedWorkOrders() {
        return workOrderRepository.findByStatus(WorkOrderStatus.COMPLETED);
    }

    @Transactional(readOnly = true)
    public List<Booking> findBookableBookings() {
        return bookingRepository.findBookable(BookingStatus.BOOKED, WorkOrderStatus.CANCELLED);
    }

    // Creates a planned work order for a booking and saves it as a draft.
    // The booking's suggested mechanic, time and services, with agreed prices, are inherited from the booking.
    // If the mechanic has a clash, the draft is saved anyway and the result contains the warning and the next available time.
    @PreAuthorize("@access.can('WORK_ORDER_WRITE')") // Ensures that only users with 'WORK_ORDER_WRITE' permission can create work orders.
    public WorkOrderScheduleResult createWorkOrder(@Positive(message = "error.notFound") int bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking", bookingId));

        if (workOrderRepository.existsByBookingIdAndStatusNot(bookingId, WorkOrderStatus.CANCELLED)) {
            throw new ValidationException("error.workOrder.alreadyExists", String.valueOf(bookingId));
        }

        WorkOrder workOrder = plannedWorkOrderFor(booking);

        WorkOrder saved = workOrderRepository.save(workOrder);
        try (MDC.MDCCloseable ignored = MDC.putCloseable(LogKeys.WORK_ORDER_ID, String.valueOf(saved.getId()))) {
            log.info("Created work order");
        }
        return scheduleResult(saved);
    }

    // Shared by createWorkOrder and reopenWorkOrder: the booking must be free and the vehicle must exist.
    // The order is created from the booking as it looks now, and the booking is locked.
    // The suggested mechanic is not looked up: the foreign key guarantees that it exists.
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

    // Creates a warranty order from a previous work order and saves it as a draft.
    @PreAuthorize("@access.can('WORK_ORDER_WRITE')")
    public WarrantyWorkOrder createWarranty(@Positive int originalWorkOrderId,
                                            String problemDescription) {
        WorkOrder original = require(originalWorkOrderId);

        WarrantyWorkOrder warranty =
                WarrantyWorkOrder.draft(original, problemDescription);

        WarrantyWorkOrder saved = workOrderRepository.save(warranty);
        try (MDC.MDCCloseable ignored = MDC.putCloseable(LogKeys.WORK_ORDER_ID, String.valueOf(saved.getId()))) {
            log.info("Created warranty work order from {}", originalWorkOrderId);
        }
        return saved;
    }

    // Creates a drop-in order (car without a booking) and saves it as a draft.
    // The service checks that the vehicle exists, the model validates the rest.
    // Services are optional for a draft; null is treated as "no services".
    @Override
    @PreAuthorize("@access.can('WORK_ORDER_WRITE')")
    public DropInWorkOrder createDropIn(int vehicleId,
                                        String problemDescription,
                                        List<Integer> serviceItemIds) {

        if (!vehicleRepo.existsById(vehicleId)) {
            throw new EntityNotFoundException("Vehicle", vehicleId);
        }

        // The services are copied as snapshots so later price changes do not affect the order.
        DropInWorkOrder dropIn = DropInWorkOrder.draft(vehicleId, problemDescription);
        dropIn.replaceItems(serviceItemRepository.requireAllById(serviceItemIds));

        DropInWorkOrder saved = workOrderRepository.save(dropIn);

        try (MDC.MDCCloseable ignored = MDC.putCloseable(LogKeys.WORK_ORDER_ID, String.valueOf(saved.getId()))) {
            log.info("Created drop-in work order");
        }
        return saved;
    }

    @Override
    @PreAuthorize("@access.can('WORK_ORDER_WRITE')")
    public void startWorkOrder(int id) {
        try (MDC.MDCCloseable ignored = MDC.putCloseable(LogKeys.WORK_ORDER_ID, String.valueOf(id))) {
            log.info("Starting work order");
            WorkOrder workOrder = require(id);
            workOrder.start();
            syncBooking(workOrder);
            workOrderRepository.save(workOrder);
        }
    }

    @Override
    @PreAuthorize("@access.can('WORK_ORDER_WRITE')")
    public void completeWorkOrder(int id) {
        try (MDC.MDCCloseable ignored = MDC.putCloseable(LogKeys.WORK_ORDER_ID, String.valueOf(id))) {
            log.info("Completing work order");
            WorkOrder workOrder = require(id);
            workOrder.complete();
            syncBooking(workOrder);
            workOrderRepository.save(workOrder);
        }
    }

    // The booking's status follows the work order. Fetching, the rule and saving live in BookingService,
    // the same path as when the plan follows the order (followBooking).
    // A work order without a booking (drop-in, warranty claim) has no booking to update.
    private void syncBooking(WorkOrder workOrder) {
        Integer bookingId = workOrder.getBookingId();
        if (bookingId == null) {
            return;
        }
        bookingService.followWorkOrderStatus(bookingId, workOrder.getStatus());
    }

    // Confirmation reserves the mechanic, so a clash stops it and nothing is saved.
    @Override
    @PreAuthorize("@access.can('WORK_ORDER_WRITE')")
    public void confirmWorkOrder(int id) {
        try (MDC.MDCCloseable ignored = MDC.putCloseable(LogKeys.WORK_ORDER_ID, String.valueOf(id))) {
            log.info("Confirming work order");
            WorkOrder workOrder = require(id);
            workOrder.confirm();
            rejectConflictsIfReserved(scheduleResult(workOrder));
            syncBooking(workOrder);
            workOrderRepository.save(workOrder);
        }
    }

    // A cancelled planned order is opened as a new order from the booking as it looks now, because the booking
    // may have changed while the order was cancelled. The cancelled order remains as history. Comments and
    // customer instructions are carried over, and the result lists what differs from the cancelled order.
    // Other orders are reopened as drafts (the same order). The booking then remains locked: DRAFT maps to WORK_ORDER_CREATED.
    @PreAuthorize("@access.can('WORK_ORDER_WRITE')")
    public WorkOrderReopenResult reopenWorkOrder(@Positive int id) {
        try (MDC.MDCCloseable ignored = MDC.putCloseable(LogKeys.WORK_ORDER_ID, String.valueOf(id))) {
            log.info("Reopening work order");
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
        try (MDC.MDCCloseable ignored = MDC.putCloseable(LogKeys.WORK_ORDER_ID, String.valueOf(saved.getId()))) {
            log.info("Recreated work order from cancelled {}", cancelled.getId());
        }
        return new WorkOrderReopenResult(scheduleResult(saved), WorkOrderChange.between(cancelled, saved));
    }

    // Cancels the order and releases the booking (BOOKED), so it can get a new order, e.g. with another mechanic.
    @Override
    @PreAuthorize("@access.can('WORK_ORDER_WRITE')")
    public void cancelWorkOrder(int id) {
        try (MDC.MDCCloseable ignored = MDC.putCloseable(LogKeys.WORK_ORDER_ID, String.valueOf(id))) {
            log.info("Cancelling work order");
            WorkOrder workOrder = require(id);
            workOrder.cancel();
            syncBooking(workOrder);
            workOrderRepository.save(workOrder);
        }
    }

    // Mechanic is optional. A draft that clashes is saved with a warning; a confirmed order that clashes is stopped.
    @Override
    @PreAuthorize("@access.can('WORK_ORDER_WRITE')")
    public WorkOrderScheduleResult updateWorkOrder(int id,
                                                   List<Integer> serviceItemIds,
                                                   Integer mechanicId,
                                                   LocalDate plannedDate,
                                                   LocalTime plannedStartTime,
                                                   String customerInstructions,
                                                   String comments,
                                                   Map<Integer, Boolean> chargeFlags) {

        try (MDC.MDCCloseable ignored = MDC.putCloseable(LogKeys.WORK_ORDER_ID, String.valueOf(id))) {
            log.info("Updating work order");
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

            // A confirmed order may not be edited into an incomplete one
            workOrder.requireCompleteIfConfirmed();

            WorkOrderScheduleResult result = scheduleResult(workOrder);

            rejectConflictsIfReserved(result);
            followBooking(workOrder);
            workOrderRepository.save(workOrder);
            return result;
        }
    }

    // The booking follows the order's date, start time and services so the calendar and the vehicle check are correct.
    // The mechanic does not follow: the order owns it, and the booking's suggestion is the customer's wish.
    // The vehicle check and the loading of services live in BookingService and are not duplicated here.
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

    // The only place that fetches clashes for the mechanic. The rule and the suggestion live in WorkOrder.
    // Without a mechanic or date there is nothing to clash with and the database is not queried.
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

    // An order that reserves the mechanic (confirmed or in progress) may not clash. A draft may, with a warning.
    private void rejectConflictsIfReserved(WorkOrderScheduleResult result) {
        WorkOrder workOrder = result.getWorkOrder();
        if (workOrder.getStatus().reservesMechanic() && !result.getMechanicConflicts().isEmpty()) {
            throw new MechanicDoubleBookingException(workOrder.getMechanicId(),
                    workOrder.getPlannedDate(), workOrder.getPlannedStartTime(), workOrder.getPlannedEndTime());
        }
    }
}
