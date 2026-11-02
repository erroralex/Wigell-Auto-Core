package com.wac.autocore.repository;

import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * <b>WorkOrderRepository</b>
 * <p>Responsibility: Database access for work orders. Spring Data generates the implementation
 * from the method names, so no SQL queries need to be written by hand.</p>
 */
@org.springframework.stereotype.Repository
public interface WorkOrderRepository extends org.springframework.data.repository.Repository<WorkOrder, Integer> {

    List<WorkOrder> findAll();
    Optional<WorkOrder> findById(Integer id);
    <S extends WorkOrder> S save(S workOrder);

    List<WorkOrder> findByStatus(WorkOrderStatus status);
    List<WorkOrder> findByMechanicId(int mechanicId);
    List<WorkOrder> findByMechanicIdAndPlannedDate(int mechanicId, LocalDate plannedDate);


    boolean existsByBookingIdAndStatusNot(int bookingId, WorkOrderStatus status);

    @Query("SELECT w FROM WorkOrder w WHERE w.status = 'COMPLETED' " +
            "AND NOT EXISTS (SELECT i FROM Invoice i WHERE i.workOrderId = w.id)")
    List<WorkOrder> findCompletedNotInvoiced();
}