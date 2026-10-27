package com.wac.autocore.service;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.MechanicRepository;
import com.wac.autocore.repository.WorkOrderRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@PreAuthorize("@access.can('DATA_READ')") // Ensures that only users with 'DATA_READ' permission can access the service methods.
public class MechanicService {

    private final MechanicRepository mechanicRepository;
    private final BookingRepository bookingRepository;
    private final WorkOrderRepository workOrderRepository;

    public MechanicService(MechanicRepository mechanicRepository, BookingRepository bookingRepository,
                           WorkOrderRepository workOrderRepository) {
        this.mechanicRepository = mechanicRepository;
        this.bookingRepository = bookingRepository;
        this.workOrderRepository = workOrderRepository;
    }

    public List<Mechanic> listAll() {
        return mechanicRepository.findAll();
    }

    public Optional<Mechanic> findById(Integer id) {
        return mechanicRepository.findById(id);
    }

    // Busy right now = a work order that reserves the mechanic is in progress according to the planned time (the rule lives in WorkOrder).
    // The booking's mechanic is only a suggestion and does not count.
    public boolean isBusyNow(Integer mechanicId) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        return workOrderRepository.findByMechanicIdAndPlannedDate(mechanicId, today).stream()
                .anyMatch(o -> o.reservesMechanicAt(today, now));
    }

    // Bookings where the mechanic is suggested.
    public List<Booking> listBookingsByMechanicId(int mechanicId) {
        return bookingRepository.findByMechanicId(mechanicId);
    }
}
