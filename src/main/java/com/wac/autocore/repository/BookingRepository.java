package com.wac.autocore.repository;

import com.wac.autocore.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Integer> {
    List<Booking> findByMechanicId(int id);

    boolean existsByVehicleIdAndDate(int vehicleId, LocalDate date);
}
