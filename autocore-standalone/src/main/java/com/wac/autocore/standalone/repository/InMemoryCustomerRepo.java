package com.wac.autocore.standalone.repository;

import com.wac.autocore.model.Customer;
import com.wac.autocore.repository.CustomerRepo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory {@link CustomerRepo} for running AutoCore without a database.
 */
public class InMemoryCustomerRepo implements CustomerRepo {

    private final Map<Integer, Customer> store = new LinkedHashMap<>();
    private final InMemoryIds ids = new InMemoryIds();

    @Override
    public Customer save(Customer customer) {
        if (InMemoryIds.idOf(customer) == 0) {
            InMemoryIds.setId(customer, ids.next());
        }
        store.put(InMemoryIds.idOf(customer), customer);
        return customer;
    }

    @Override
    public Optional<Customer> findById(Integer id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Customer> findAll() {
        return new ArrayList<>(store.values());
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
