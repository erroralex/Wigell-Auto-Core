package com.wac.autocore.repository;

import com.wac.autocore.model.Mechanic;

import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface MechanicRepository extends org.springframework.data.repository.Repository<Mechanic, Integer> {
    List<Mechanic> findAll();
    Optional<Mechanic> findById(Integer id);
    Mechanic save(Mechanic mechanic);
}