package com.wac.autocore.standalone.repository;

import com.wac.autocore.model.Mechanic;
import com.wac.autocore.repository.MechanicRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory {@link MechanicRepository} for running AutoCore without a database.
 */
public class InMemoryMechanicRepository implements MechanicRepository {

    private final Map<Integer, Mechanic> store = new LinkedHashMap<>();
    private final InMemoryIds ids = new InMemoryIds();

    @Override
    public List<Mechanic> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public Optional<Mechanic> findById(Integer id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Mechanic save(Mechanic mechanic) {
        if (InMemoryIds.idOf(mechanic) == 0) {
            InMemoryIds.setId(mechanic, ids.next());
        }
        store.put(InMemoryIds.idOf(mechanic), mechanic);
        return mechanic;
    }

    public Mechanic insert(Mechanic mechanic) {
        store.put(ids.next(), mechanic);
        return mechanic;
    }
}
