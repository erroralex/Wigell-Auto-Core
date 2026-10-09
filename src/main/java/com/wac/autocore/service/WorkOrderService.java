package com.wac.autocore.service;

import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.*;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.MechanicRepository;
import com.wac.autocore.repository.VehicleRepo;
import com.wac.autocore.repository.WorkOrderRepository;
import com.wac.autocore.repository.ServiceItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <b>WorkOrderService</b>
 * <p>Ansvar: Affärslogik för arbetsordrar: Skapa, starta och avsluta, samt hämtning för vyerna.</p>
 * <p>{@link #createWorkOrder(int)} kastar ett {@code DomainException} med språknyckel när
 * ordern inte kan skapas, så att vyn kan visa rätt orsak via {@code ErrorFacade}.
 * {@link #startWorkOrder(WorkOrder)} och {@link #completeWorkOrder(WorkOrder)} kastar {@code IllegalStatusTransitionException}
 * när övergången inte är tillåten.</p>
 */
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
    public WorkOrder findById(int id) {
        return workOrderRepository.findById(id).orElse(null);
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
                .filter(booking -> !workOrderRepository.existsByBookingId(booking.getId()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Booking> findAllBookings() {
        return bookingRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Mechanic> findAllMechanics() {
        return mechanicRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ServiceItem> findAllServiceItems() {
        return serviceItemRepository.findAll();
    }

    // Skapar en planerad arbetsorder för en bokning, ordern returneras bekräftad.
    // Mekaniker och alla tjänster, med avtalade priser, ärvs från bokningen.
    public WorkOrder createWorkOrder(int bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking", bookingId));

        if (workOrderRepository.existsByBookingId(bookingId)) {
            throw new ValidationException("error.workOrderExists", String.valueOf(bookingId));
        }

        if (!Booking.STATUS_BOOKED.equals(booking.getStatus())) {
            throw new ValidationException("error.workOrderBookingStatus", String.valueOf(bookingId));
        }

        // Mekanikern reserverades redan när bokningen skapades
        if (!mechanicRepository.existsById(booking.getMechanicId())) {
            throw new EntityNotFoundException("Mechanic", booking.getMechanicId());
        }

        WorkOrder workOrder = PlannedWorkOrder.createFrom(booking);

        booking.setStatus(BOOKING_STATUS_WORK_ORDER_CREATED);
        bookingRepository.save(booking);
        return workOrderRepository.save(workOrder);
    }

    // Skapar en garantiorder från en tidigare arbetsorder och sparar den som utkast.
    public WarrantyWorkOrder createWarranty(int originalWorkOrderId,
                                            String problemDescription) {
        WorkOrder original = workOrderRepository.findById(originalWorkOrderId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "WorkOrder", originalWorkOrderId
                ));

        WarrantyWorkOrder warranty =
                WarrantyWorkOrder.draft(original, problemDescription);

        return workOrderRepository.save(warranty);
    }

    // Skapar en drop-in-order (bil utan bokning) och sparar den som utkast.
    // Servicen kontrollerar att fordonet finns, modellen validerar resten.
    public DropInWorkOrder createDropIn(int vehicleId,
                                        String problemDescription,
                                        List<Integer> serviceItemIds) {
        if (!vehicleRepo.existsById(vehicleId)) {
            throw new EntityNotFoundException("Vehicle", vehicleId);
        }

        // Tjänsterna kopieras som snapshots så senare prisändringar inte påverkar ordern.
        DropInWorkOrder dropIn = DropInWorkOrder.draft(vehicleId, problemDescription);
        for (int serviceItemId : serviceItemIds) {
            ServiceItem service = serviceItemRepository.findById(serviceItemId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "ServiceItem", serviceItemId, "error.serviceNotFound"));
            dropIn.addService(service);
        }
        return workOrderRepository.save(dropIn);
    }

    public void startWorkOrder(WorkOrder workOrder) {
        workOrder.start();
        syncBookingStatus(workOrder, WorkOrderStatus.IN_PROGRESS);
        workOrderRepository.save(workOrder);
    }

    public void completeWorkOrder(WorkOrder workOrder) {
        workOrder.complete();
        syncBookingStatus(workOrder, WorkOrderStatus.COMPLETED);
        workOrderRepository.save(workOrder);
    }



    // En arbetsorder utan bokning (drop-in) har ingen bokningsstatus att uppdatera
    private void syncBookingStatus(WorkOrder workOrder, WorkOrderStatus status) {
        Integer bookingId = workOrder.getBookingId();
        if (bookingId == null) {
            return;
        }

        bookingRepository.findById(bookingId).ifPresent(booking -> {
            booking.setStatus(status.name());
            bookingRepository.save(booking);
        });
    }

    public void confirmWorkOrder(WorkOrder workOrder) {
        workOrder.confirm();
        syncBookingStatus(workOrder, WorkOrderStatus.CONFIRMED);
        workOrderRepository.save(workOrder);
    }
}
