package com.wac.autocore.service;

import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.Vehicle;
import com.wac.autocore.repository.CustomerRepo;
import com.wac.autocore.repository.VehicleRepo;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Optional;

@Validated
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

    public boolean existsByRegistrationNumber(@NotBlank(message="error.vehicle.blankRegNr") String registrationNumber) {
        return vehicleRepo.existsByRegistrationNumber(registrationNumber);
    }

    public Vehicle create(@NotBlank(message="error.vehicle.blankRegNr")String registrationNumber,
                          @NotBlank(message="error.vehicle.blankBrand")String brand,
                          @NotBlank(message="error.vehicle.blankModel")String model,
                          @Positive @NotNull(message="error.vehicle.yearIsNull") Integer year,
                          @Positive @NotNull(message="error.vehicle.customerIdIsNull") Integer customerId) {
        validateCustomer(customerId);

        if (vehicleRepo.existsByRegistrationNumber(registrationNumber)) {
            throw new ValidationException("error.regNumberExists", registrationNumber);
        }

        return vehicleRepo.save(
                new Vehicle(registrationNumber, brand, model, year, customerId)
        );
    }

    public Vehicle update(@Positive @NotNull(message="error.vehicle.idIsNull") Integer id,
                          @NotBlank(message="error.vehicle.blankRegNr")String registrationNumber,
                          @NotBlank(message="error.vehicle.blankBrand") String brand,
                          @NotBlank(message="error.vehicle.blankModel") String model,
                          @Positive Integer year,
                          @Positive @NotNull(message="error.vehicle.customerIdIsNull") Integer customerId) {


        Vehicle vehicle = vehicleRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("vehicle", id));

        validateCustomer(customerId);

        vehicle.setRegistrationNumber(registrationNumber);
        vehicle.setBrand(brand);
        vehicle.setModel(model);
        vehicle.setYear(year);
        vehicle.setCustomerId(customerId);

        vehicleRepo.findByRegistrationNumber(vehicle.getRegistrationNumber())
                .filter(existing -> existing.getId() != vehicle.getId())
                .ifPresent(existing -> {
                    throw new ValidationException("error.regNumberExists", vehicle.getRegistrationNumber());
                });

        return vehicleRepo.save(vehicle);
    }

    public void deleteById(@Positive @NotNull(message="error.vehicle.idIsNull") Integer id) {
        vehicleRepo.deleteById(id);
    }

    private void validateCustomer(Integer customerId) {
        if (!customerRepo.existsById(customerId)) {
            throw new EntityNotFoundException("Customer", customerId);
        }
    }

}
