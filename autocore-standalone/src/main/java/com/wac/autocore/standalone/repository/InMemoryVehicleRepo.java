package com.wac.autocore.standalone.repository;

import com.wac.autocore.model.Vehicle;
import com.wac.autocore.repository.VehicleRepo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * In-memory {@link VehicleRepo} for running AutoCore without a database.
 */
public class InMemoryVehicleRepo implements VehicleRepo {

    private final Map<Integer, Vehicle> store = new LinkedHashMap<>();
    private final InMemoryIds ids = new InMemoryIds();

    @Override
    public List<Vehicle> findByCustomerId(int customerId) {
        return store.values().stream()
                .filter(v -> v.getCustomerId() == customerId)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByRegistrationNumber(String registrationNumber) {
        return store.values().stream()
                .anyMatch(v -> v.getRegistrationNumber().equals(registrationNumber));
    }

    @Override
    public Optional<Vehicle> findByRegistrationNumber(String registrationNumber) {
        return store.values().stream()
                .filter(v -> v.getRegistrationNumber().equals(registrationNumber))
                .findFirst();
    }

    @Override
    public List<Vehicle> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public Optional<Vehicle> findById(Integer id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public <S extends Vehicle> S save(S vehicle) {
        if (InMemoryIds.idOf(vehicle) == 0) {
            InMemoryIds.setId(vehicle, ids.next());
        }
        store.put(InMemoryIds.idOf(vehicle), vehicle);
        return vehicle;
    }

    @Override
    public boolean existsById(Integer id) {
        return store.containsKey(id);
    }

    @Override
    public void deleteById(Integer id) {
        store.remove(id);
    }
}
