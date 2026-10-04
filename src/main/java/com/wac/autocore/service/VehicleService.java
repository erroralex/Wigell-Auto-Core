package com.wac.autocore.service;

import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.repository.CustomerRepo;
import com.wac.autocore.repository.VehicleRepo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class VehicleService {

    private final VehicleRepo vehicleRepo;
    private final CustomerRepo customerRepo;

    public VehicleService(VehicleRepo vehicleRepo, CustomerRepo customerRepo) {
        this.vehicleRepo = vehicleRepo;
        this.customerRepo = customerRepo;
    }

    public List<Vehicle> findAll() {
        return vehicleRepo.findAll();
    }

    public Optional<Vehicle> findById(Integer id) {
        return vehicleRepo.findById(id);
    }

    public List<Vehicle> findByCustomerId(Integer customerId) {
        return vehicleRepo.findByCustomerId(customerId);
    }

    public boolean existsById(Integer id) {
        return vehicleRepo.existsById(id);
    }

    public boolean existsByRegistrationNumber(String registrationNumber) {
        return vehicleRepo.existsByRegistrationNumber(registrationNumber);
    }

    public Vehicle create(String registrationNumber,
                          String brand,
                          String model,
                          int year,
                          int customerId) {
        validateCustomer(customerId);

        if (vehicleRepo.existsByRegistrationNumber(registrationNumber)) {
            throw new ValidationException("error.regNumberExists", registrationNumber);
        }

        return vehicleRepo.save(
                new Vehicle(registrationNumber, brand, model, year, customerId)
        );
    }

    public Vehicle update(Vehicle vehicle) {
        if (!vehicleRepo.existsById(vehicle.getId())) {
            throw new EntityNotFoundException("Vehicle", vehicle.getId());
        }

        validateCustomer(vehicle.getCustomerId());
        vehicleRepo.findByRegistrationNumber(vehicle.getRegistrationNumber())
                .filter(existing -> existing.getId() != vehicle.getId())
                .ifPresent(existing -> {
                    throw new ValidationException("error.regNumberExists", vehicle.getRegistrationNumber());
                });

        return vehicleRepo.save(vehicle);
    }

    public void deleteById(Integer id) {
        vehicleRepo.deleteById(id);
    }

    private void validateCustomer(int customerId) {
        if (!customerRepo.existsById(customerId)) {
            throw new EntityNotFoundException("Customer", customerId);
        }
    }

}
