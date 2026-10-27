package com.wac.autocore.standalone.repository;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.BookingStatus;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.repository.BookingRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class InMemoryBookingRepository implements BookingRepository {

    private final Map<Integer, Booking> store = new LinkedHashMap<>();
    private final InMemoryIds ids = new InMemoryIds();
    private final InMemoryWorkOrderRepository workOrderRepository;

    public InMemoryBookingRepository(InMemoryWorkOrderRepository workOrderRepository) {
        this.workOrderRepository = workOrderRepository;
    }

    @Override
    public List<Booking> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public Optional<Booking> findById(Integer id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Booking save(Booking booking) {
        if (InMemoryIds.idOf(booking) == 0) {
            InMemoryIds.setId(booking, ids.next());
        }
        store.put(InMemoryIds.idOf(booking), booking);
        return booking;
    }

    @Override
    public List<Booking> findByMechanicId(int id) {
        return store.values().stream()
                .filter(b -> b.getMechanicId() == id)
                .collect(Collectors.toList());
    }

    @Override
    public List<Booking> findByStatusNot(BookingStatus status) {
        return store.values().stream()
                .filter(b -> b.getStatus() != status)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByVehicleIdAndDateAndStatusNot(int vehicleId, LocalDate date, BookingStatus status) {
        return store.values().stream()
                .anyMatch(b -> b.getVehicleId() == vehicleId
                        && b.getDate().equals(date)
                        && b.getStatus() != status);
    }

    @Override
    public List<Booking> findBookable(BookingStatus status, WorkOrderStatus cancelled) {
        return store.values().stream()
                .filter(b -> b.getStatus() == status)
                .filter(b -> !workOrderRepository.existsByBookingIdAndStatusNot(b.getId(), cancelled))
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByVehicleIdAndDateAndIdNotAndStatusNot(int vehicleId, LocalDate date, int excludedBookingId, BookingStatus status) {
        return store.values().stream()
                .anyMatch(b -> b.getVehicleId() == vehicleId
                        && b.getDate().equals(date)
                        && b.getId() != excludedBookingId
                        && b.getStatus() != status);
    }
}