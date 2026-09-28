package com.wac.autocore.repository;

import com.wac.autocore.model.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * <b>WorkOrderRepository</b>
 * <p>Ansvar: Databasåtkomst för arbetsordrar. Spring Data genererar implementationen
 * utifrån metodnamnen, så inga SQL-frågor behöver skrivas för hand.</p>
 */
@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, Integer> {

    List<WorkOrder> findByStatus(String status);
    List<WorkOrder> findByMechanicId(int mechanicId);
    Optional<WorkOrder> findByBookingId(int bookingId);
    boolean existsByBookingId(int bookingId);

    @Query("SELECT w FROM WorkOrder w WHERE w.status = 'COMPLETED' " +
            "AND NOT EXISTS (SELECT i FROM Invoice i WHERE i.workOrderId = w.id)")
    List<WorkOrder> findCompletedNotInvoiced();
}
