package com.wac.autocore.service;

import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.*;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.MechanicRepository;
import com.wac.autocore.repository.ServiceItemRepository;
import com.wac.autocore.repository.WorkOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <b>WorkOrderService</b>
 * <p>Ansvar: Affärslogik för arbetsordrar: Skapa, starta och avsluta, samt hämtning för vyerna.</p>
 * <p>{@link #createWorkOrder(int)} kastar ett {@code DomainException} med språknyckel när
 * ordern inte kan skapas, så att vyn kan visa rätt orsak via {@code ErrorFacade}.
 * {@link #startWorkOrder(int)} och {@link #completeWorkOrder(int)} returnerar {@code false}
 * när övergången inte är tillåten.</p>
 */
@Service
@Transactional
public class WorkOrderService {

    public static final String STATUS_CREATED = "CREATED";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";

    private static final String BOOKING_STATUS_WORK_ORDER_CREATED = "WORK_ORDER_CREATED";

    private final WorkOrderRepository workOrderRepository;
    private final BookingRepository bookingRepository;
    private final MechanicRepository mechanicRepository;

    public WorkOrderService(WorkOrderRepository workOrderRepository,
                            BookingRepository bookingRepository,
                            MechanicRepository mechanicRepository) {
        this.workOrderRepository = workOrderRepository;
        this.bookingRepository = bookingRepository;
        this.mechanicRepository = mechanicRepository;
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

    // Skapar en arbetsorder för en bokning. Mekaniker och alla tjänster, med avtalade priser, ärvs från bokningen.
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

        WorkOrder workOrder = WorkOrder.createFrom(booking);

        booking.setStatus(BOOKING_STATUS_WORK_ORDER_CREATED);
        bookingRepository.save(booking);
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
            booking.setStatus(STATUS_IN_PROGRESS);
            bookingRepository.save(booking);
        });

        workOrderRepository.save(workOrder);
        return true;
    }

    // Avslutar en arbetsorder. Tillåts bara från status IN_PROGRESS.
    public boolean completeWorkOrder(int workOrderId) {
        WorkOrder workOrder = findById(workOrderId);
        if (workOrder == null || !STATUS_IN_PROGRESS.equals(workOrder.getStatus())) {
            return false;
        }

        workOrder.setStatus(STATUS_COMPLETED);

        bookingRepository.findById(workOrder.getBookingId()).ifPresent(booking -> {
            booking.setStatus(STATUS_COMPLETED);
            bookingRepository.save(booking);
        });

        workOrderRepository.save(workOrder);
        return true;
    }
}