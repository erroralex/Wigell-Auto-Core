package com.wac.autocore.repository;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.BookingStatus;
import com.wac.autocore.model.WorkOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Integer> {
    // Bokningar där mekanikern är föreslagen. Mekanikerns reserverade tid finns på arbetsordern.
    List<Booking> findByMechanicId(int id);

    // Används med CANCELLED för att lista aktiva bokningar.
    List<Booking> findByStatusNot(BookingStatus status);

    // Fordonskontrollen anropas med CANCELLED, så att en avbokad bokning inte blockerar fordonet.
    boolean existsByVehicleIdAndDateAndStatusNot(int vehicleId, LocalDate date, BookingStatus status);

    @Query("SELECT b FROM Booking b " +
            "WHERE b.status = :status " +
            "AND NOT EXISTS (" +
            "  SELECT w FROM WorkOrder w " +
            "  WHERE w.bookingId = b.id " +
            "  AND w.status <> :cancelled)")
    List<Booking> findBookable(@Param("status") BookingStatus status,
                               @Param("cancelled") WorkOrderStatus cancelled);
    // excludedBookingId är bokningen som uppdateras och som kontrollen ska ignorera.
    boolean existsByVehicleIdAndDateAndIdNotAndStatusNot(int vehicleId, LocalDate date, int excludedBookingId,
                                                         BookingStatus status);
}
