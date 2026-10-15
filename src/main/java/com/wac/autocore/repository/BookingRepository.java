package com.wac.autocore.repository;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.BookingStatus;
import com.wac.autocore.model.WorkOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Integer> {
    List<Booking> findByMechanicId(int id);

    boolean existsByVehicleIdAndDate(int vehicleId, LocalDate date);

    List<Booking> findBookingByMechanicIdAndDate(int mechanicId, LocalDate date);

    @Query("SELECT b FROM Booking b " +
            "WHERE b.status = :status " +
            "AND NOT EXISTS (" +
            "  SELECT w FROM WorkOrder w " +
            "  WHERE w.bookingId = b.id " +
            "  AND w.status <> :cancelled)")
    List<Booking> findBookable(@Param("status") BookingStatus status,
                               @Param("cancelled") WorkOrderStatus cancelled);
}
