package com.wac.autocore.service;

import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.*;
import com.wac.autocore.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import sun.awt.image.ImageWatched;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.time.LocalDate;
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

    private static final String BOOKING_STATUS_WORK_ORDER_CREATED = "WORK_ORDER_CREATED";

    private final WorkOrderRepository workOrderRepository;
    private final BookingRepository bookingRepository;
    private final MechanicRepository mechanicRepository;
    private final VehicleRepo vehicleRepo;
    private final ServiceItemRepository serviceItemRepository;

    public WorkOrderService(WorkOrderRepository workOrderRepository,
                            BookingRepository bookingRepository,
                            MechanicRepository mechanicRepository,
                            VehicleRepo vehicleRepo,
                            ServiceItemRepository serviceItemRepository) {
        this.workOrderRepository = workOrderRepository;
        this.bookingRepository = bookingRepository;
        this.mechanicRepository = mechanicRepository;
        this.vehicleRepo = vehicleRepo;
        this.serviceItemRepository = serviceItemRepository;
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
        return bookingRepository.findAll().stream()
                .filter(booking -> Booking.STATUS_BOOKED.equals(booking.getStatus()))
                .filter(booking -> !workOrderRepository.existsByBookingIdAndStatusNot(booking.getId(), WorkOrderStatus.CANCELLED))
                .collect(Collectors.toList());
    }

    // Skapar en planerad arbetsorder för en bokning, ordern returneras bekräftad.
    // Mekaniker och alla tjänster, med avtalade priser, ärvs från bokningen.
    public WorkOrder createWorkOrder(@Positive(message = "error.notFound") int bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking", bookingId));

        if (workOrderRepository.existsByBookingIdAndStatusNot(bookingId, WorkOrderStatus.CANCELLED)) {
            throw new ValidationException("error.workOrder.alreadyExists", String.valueOf(bookingId));
        }

        if (!Booking.STATUS_BOOKED.equals(booking.getStatus())) {
            throw new ValidationException("error.workOrder.bookingNotBookable", String.valueOf(bookingId));
        }

        // Mekanikern reserverades redan när bokningen skapades
        if (!mechanicRepository.existsById(booking.getMechanicId())) {
            throw new EntityNotFoundException("Mechanic", booking.getMechanicId());
        }

        if (!vehicleRepo.existsById(booking.getVehicleId())) {
            throw new EntityNotFoundException("Vehicle", booking.getVehicleId());
        }

        WorkOrder workOrder = PlannedWorkOrder.createFrom(booking);

        syncBooking(workOrder);
        return workOrderRepository.save(workOrder);
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
                                        @NotBlank(message = "error.workOrder.missingProblemDescription") String problemDescription,
                                        List<Integer> serviceItemIds) {
        if (!vehicleRepo.existsById(vehicleId)) {
            throw new EntityNotFoundException("Vehicle", vehicleId);
        }

        // Tjänsterna kopieras som snapshots så senare prisändringar inte påverkar ordern.
        DropInWorkOrder dropIn = DropInWorkOrder.draft(vehicleId, problemDescription);

        Set<Integer> serviceIds = serviceItemIds == null
                ? Collections.emptySet()
                : new LinkedHashSet<>(serviceItemIds);

        for (Integer serviceItemId : serviceIds) {
            ServiceItem service = serviceItemRepository.findById(serviceItemId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "ServiceItem", serviceItemId, "error.serviceNotFound"));
            dropIn.addItem(service);
        }
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

    // Uppdaterar bokningens status så den följer arbetsordern.
    // En arbetsorder utan bokning (drop-in, reklamation) har ingen bokning att uppdatera.
    private void syncBooking(WorkOrder workOrder) {
        Integer bookingId = workOrder.getBookingId();
        if (bookingId == null) {
            return;
        }
        bookingRepository.findById(bookingId).ifPresent(booking -> {
            booking.followWorkOrder(workOrder.getStatus());
            bookingRepository.save(booking);
        });
    }

    public void confirmWorkOrder(@Positive int id) {
        WorkOrder workOrder = require(id);
        workOrder.confirm();
        syncBooking(workOrder);
        workOrderRepository.save(workOrder);
    }

    // Återöppnar ordern som utkast och sätter bokningen till BOOKED så att den går att ändra.
    // Om ordern är avbruten måste bokningen först ha blivit ledig: har bokningen redan fått en ny order
    // som inte är avbruten (t.ex efter att den avbrutna ordern ersatts) blockeras återöppningen,
    // annars skulle bokningen få två pågående ordrar.
    // Kontrollen sker före reopen(), annars skulle ordern räknas som aktiv.
    public void reopenWorkOrder(@Positive int id) {
        WorkOrder workOrder = require(id);
        Integer bookingId = workOrder.getBookingId();
        if (bookingId != null && workOrder.getStatus() == WorkOrderStatus.CANCELLED
                && workOrderRepository.existsByBookingIdAndStatusNot(bookingId, WorkOrderStatus.CANCELLED)) {
            throw new ValidationException("error.workOrder.bookingHasActiveOrder", String.valueOf(bookingId));
        }
        workOrder.reopen();
        syncBooking(workOrder);
        workOrderRepository.save(workOrder);
    }

    // Avbryter ordern och släpper bokningen (BOOKED), så den kan få en ny order, t.ex med annan mekaniker.
    public void cancelWorkOrder(@Positive int id) {
        WorkOrder workOrder = require(id);
        workOrder.cancel();
        syncBooking(workOrder);
        workOrderRepository.save(workOrder);
    }

    public void updateWorkOrder(@Positive int id,
                                @NotEmpty(message = "error.workOrder.missingServices")
                                List<@NotNull @Positive Integer> serviceItemIds,

                                @NotNull(message = "error.workOrder.missingMechanic")
                                @Positive Integer mechanicId,

                                LocalDate plannedDate,
                                String customerInstructions,
                                String comments,
                                Map<Integer, Boolean> chargeFlags) {
        WorkOrder workOrder = require(id);

        if (!mechanicRepository.existsById(mechanicId)) {
            throw new EntityNotFoundException("Mechanic", mechanicId);
        }

        Set<Integer> wanted = new HashSet<>(serviceItemIds);
        Set<Integer> existing = workOrder.getItems().stream()
                .map(WorkOrderItem::getServiceItemId)
                .collect(Collectors.toSet());

        for (int serviceItemId : wanted) {
            if (!existing.contains(serviceItemId)) {
                ServiceItem service = serviceItemRepository.findById(serviceItemId)
                        .orElseThrow(() -> new EntityNotFoundException(
                                "ServiceItem", serviceItemId, "error.serviceNotFound"));
                workOrder.addItem(service);
            }
        }
        for (int serviceItemId : existing) {
            if (!wanted.contains(serviceItemId)) {
                workOrder.removeItem(serviceItemId);
            }
        }

        workOrder.applyChargeFlags(chargeFlags);

        workOrder.setMechanicId(mechanicId);
        workOrder.setPlannedDate(plannedDate);
        workOrder.setCustomerInstructions(customerInstructions);
        workOrder.setComments(comments);

        workOrderRepository.save(workOrder);
    }
}
