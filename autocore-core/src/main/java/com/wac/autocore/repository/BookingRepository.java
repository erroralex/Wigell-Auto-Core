package com.wac.autocore.repository;

import com.wac.autocore.model.Booking;
import com.wac.autocore.model.BookingStatus;
import com.wac.autocore.model.WorkOrderStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface BookingRepository extends org.springframework.data.repository.Repository<Booking, Integer> {

    List<Booking> findAll();
    Optional<Booking> findById(Integer id);
    <S extends Booking> S save(S booking);

    // Bookings where the mechanic is suggested. The mechanic's reserved time is on the work order.
    List<Booking> findByMechanicId(int id);

    // Used with CANCELLED to list active bookings.
    List<Booking> findByStatusNot(BookingStatus status);

    // The vehicle check is called with CANCELLED, so a cancelled booking does not block the vehicle.
    boolean existsByVehicleIdAndDateAndStatusNot(int vehicleId, LocalDate date, BookingStatus status);

    @Query("SELECT b FROM Booking b " +
            "WHERE b.status = :status " +
            "AND NOT EXISTS (" +
            "  SELECT w FROM WorkOrder w " +
            "  WHERE w.bookingId = b.id " +
            "  AND w.status <> :cancelled)")
    List<Booking> findBookable(@Param("status") BookingStatus status,
                               @Param("cancelled") WorkOrderStatus cancelled);
    // excludedBookingId is the booking being updated, which the check should ignore.
    boolean existsByVehicleIdAndDateAndIdNotAndStatusNot(int vehicleId, LocalDate date, int excludedBookingId,
                                                         BookingStatus status);
}