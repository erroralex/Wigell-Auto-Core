package com.wac.autocore.repository;

import com.wac.autocore.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Integer> {
    List<Booking> findByMechanicId(int id);

    boolean existsByVehicleIdAndDate(int vehicleId, LocalDate date);

    List<Booking> findBookingByMechanicIdAndDate(int mechanicId, LocalDate date);

    boolean existsByVehicleIdAndDateAndIdNot(int vehicleId, LocalDate date, int excludedBookingId); // excludedBookingId är ID:t av en bokning som checken ska ignorera när den letar efter konflikten.
}
