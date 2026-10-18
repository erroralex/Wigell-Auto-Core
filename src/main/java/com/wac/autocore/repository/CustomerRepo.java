package com.wac.autocore.repository;

import com.wac.autocore.model.Customer;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface CustomerRepo extends Repository<Customer, Integer> {
    Customer save(Customer customer);
    Optional<Customer> findById(Integer id);
    List<Customer> findAll();
    boolean existsById(Integer id);
    void deleteById(Integer id);
}