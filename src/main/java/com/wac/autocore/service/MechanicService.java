package com.wac.autocore.service;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.Mechanic;
import com.wac.autocore.repository.BookingRepository;
import com.wac.autocore.repository.MechanicRepository;
import com.wac.autocore.repository.WorkOrderRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
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

    public Optional<Mechanic> findById(int id) {
        return mechanicRepository.findById(id);
    }

    // Upptagen just nu = en arbetsorder som reserverar mekanikern pågår enligt planerad tid (regeln ligger i WorkOrder).
    // Bokningens mekaniker är bara ett förslag och räknas inte.
    public boolean isBusyNow(int mechanicId) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        return workOrderRepository.findByMechanicIdAndPlannedDate(mechanicId, today).stream()
                .anyMatch(o -> o.reservesMechanicAt(today, now));
    }

    // Bokningar där mekanikern är föreslagen.
    public List<Booking> listBookingsByMechanicId(int mechanicId) {
        return bookingRepository.findByMechanicId(mechanicId);
    }
}
