package com.wac.autocore.service;

import com.wac.autocore.model.Mechanic;
import com.wac.autocore.repository.MechanicRepository;
import com.wac.autocore.repository.ServiceItemRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class MechanicService {

    private final MechanicRepository mechanicRepository;

    public MechanicService(MechanicRepository mechanicRepository) {
        this.mechanicRepository = mechanicRepository;
    }

    public List<Mechanic> listAll() {
        return mechanicRepository.findAll();
    }

    public Optional<Mechanic> findById(int id) {
        return mechanicRepository.findById(id);
    }

    public boolean isAvailable(int id) {
        return mechanicRepository.findById(id)
                .map(Mechanic::isAvailable)
                .orElse(false);
    }
}
