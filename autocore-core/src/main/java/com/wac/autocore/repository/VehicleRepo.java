package com.wac.autocore.repository;

import com.wac.autocore.model.Vehicle;

import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface VehicleRepo extends org.springframework.data.repository.Repository<Vehicle, Integer> {

    List<Vehicle> findByCustomerId(int customerId);

    boolean existsByRegistrationNumber(String registrationNumber);

    Optional<Vehicle> findByRegistrationNumber(String registrationNumber);

    List<Vehicle> findAll();

    Optional<Vehicle> findById(Integer id);

    <S extends Vehicle> S save(S vehicle);

    boolean existsById(Integer id);

    void deleteById(Integer id);
}