package com.wac.autocore.service;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.MechanicRepository;
import com.wac.autocore.repository.ServiceItemRepository;
import com.wac.autocore.repository.WorkOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <b>WorkOrderService</b>
 * <p>Ansvar: Affärslogik för arbetsordrar: Skapa, starta och avsluta, samt hämtning för vyerna.
 * Metoder som misslyckas returnerar {@code null} eller {@code false} så att vyerna kan visa fel via AlertHelper.</p>
 */
@Service
@Transactional
public class WorkOrderService {

    public static final String STATUS_CREATED = "CREATED";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";

    private final WorkOrderRepository workOrderRepository;
    private final BookingRepository bookingRepository;
    private final MechanicRepository mechanicRepository;
    private final ServiceItemRepository serviceItemRepository;

    public WorkOrderService(WorkOrderRepository workOrderRepository,
                            BookingRepository bookingRepository,
                            MechanicRepository mechanicRepository,
                            ServiceItemRepository serviceItemRepository) {
        this.workOrderRepository = workOrderRepository;
        this.bookingRepository = bookingRepository;
        this.mechanicRepository = mechanicRepository;
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

    // Skapar en arbetsorder för en bokning
    public WorkOrder createWorkOrder(int bookingId, int mechanicId, int... serviceItemIds) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            return null;
        }

        if (workOrderRepository.existsByBookingId(bookingId)) {
            return null;
        }

        int resolvedMechanicId = mechanicId > 0 ? mechanicId : booking.getMechanicId();
        Mechanic mechanic = mechanicRepository.findById(resolvedMechanicId).orElse(null);
        if (mechanic == null || !mechanic.isAvailable()) {
            return null;
        }

        if (serviceItemIds == null || serviceItemIds.length == 0) {
            return null;
        }

        for (int serviceItemId : serviceItemIds) {
            if (!serviceItemRepository.existsById(serviceItemId)) {
                return null;
            }
        }

        WorkOrder workOrder = new WorkOrder(bookingId, resolvedMechanicId);
        for (int serviceItemId : serviceItemIds) {
            workOrder.addServiceItem(serviceItemId);
        }

        booking.setStatus("WORK_ORDER_CREATED");
        mechanic.setAvailable(false);

        bookingRepository.save(booking);
        mechanicRepository.save(mechanic);
        return workOrderRepository.save(workOrder);
    }

    // Startar en arbetsorder. Tillåts bara från status CREATED.
    public boolean startWorkOrder(int workOrderId) {

        WorkOrder workOrder = findById(workOrderId);
        if (workOrder == null || !STATUS_CREATED.equals(workOrder.getStatus())) {
            return false;
        }
        workOrder.setStatus(STATUS_IN_PROGRESS);

        bookingRepository.findById(workOrder.getBookingId()).ifPresent(booking -> {
            booking.setStatus("IN_PROGRESS");
            bookingRepository.save(booking);
        });

        workOrderRepository.save(workOrder);
        return true;
    }


    // Avslutar en arbetsorder. Tillåts bara från status IN_PROGRESS.
    // Mekanikern blir tillgänglig igen och bokningen markeras som klar.
    public boolean completeWorkOrder(int workOrderId) {
        WorkOrder workOrder = findById(workOrderId);
        if (workOrder == null || !STATUS_IN_PROGRESS.equals(workOrder.getStatus())) {
            return false;
        }

        workOrder.setStatus(STATUS_COMPLETED);

        mechanicRepository.findById(workOrder.getMechanicId()).ifPresent(mechanic -> {
            mechanic.setAvailable(true);
            mechanicRepository.save(mechanic);
        });

        bookingRepository.findById(workOrder.getBookingId()).ifPresent(booking -> {
            booking.setStatus("COMPLETED");
            bookingRepository.save(booking);
        });

        workOrderRepository.save(workOrder);
        return true;
    }
}
