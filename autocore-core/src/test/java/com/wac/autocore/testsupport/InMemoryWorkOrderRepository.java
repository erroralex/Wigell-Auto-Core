package com.wac.autocore.testsupport;

import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.repository.WorkOrderRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class InMemoryWorkOrderRepository implements WorkOrderRepository {

    private final Map<Integer, WorkOrder> store = new LinkedHashMap<>();
    private final InMemoryIds ids = new InMemoryIds();

    @Override
    public List<WorkOrder> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public Optional<WorkOrder> findById(Integer id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public <S extends WorkOrder> S save(S workOrder) {
        if (InMemoryIds.idOf(workOrder) == 0) {
            InMemoryIds.setId(workOrder, ids.next());
        }
        store.put(InMemoryIds.idOf(workOrder), workOrder);
        return workOrder;
    }

    @Override
    public List<WorkOrder> findByStatus(WorkOrderStatus status) {
        return store.values().stream()
                .filter(w -> w.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public List<WorkOrder> findByMechanicId(int mechanicId) {
        return store.values().stream()
                .filter(w -> w.getMechanicId() != null && w.getMechanicId() == mechanicId)
                .collect(Collectors.toList());
    }

    @Override
    public List<WorkOrder> findByMechanicIdAndPlannedDate(int mechanicId, LocalDate plannedDate) {
        return store.values().stream()
                .filter(w -> w.getMechanicId() != null && w.getMechanicId() == mechanicId)
                .filter(w -> plannedDate.equals(w.getPlannedDate()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByBookingIdAndStatusNot(int bookingId, WorkOrderStatus status) {
        return store.values().stream()
                .anyMatch(w -> w.getBookingId() != null && w.getBookingId() == bookingId && w.getStatus() != status);
    }

    @Override
    public List<WorkOrder> findCompletedNotInvoiced() {
        // Invoices are not used in the lifecycle test, so we could return empty or throw.
        // Throwing is safer so we notice if it is called.
        throw new UnsupportedOperationException("findCompletedNotInvoiced not needed in lifecycle test");
    }
}