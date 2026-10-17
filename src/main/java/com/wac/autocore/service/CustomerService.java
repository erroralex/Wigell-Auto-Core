package com.wac.autocore.service;

import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.model.Customer;
import com.wac.autocore.repository.CustomerRepo;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;


import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

@Validated
@Service
public class CustomerService {

    private final CustomerRepo customerRepo;

    public CustomerService(CustomerRepo customerRepo) {
        this.customerRepo = customerRepo;
    }

    public List<Customer> findAll() {
        return customerRepo.findAll();
    }

    public Optional<Customer> findById(Integer id) {
        return customerRepo.findById(id);
    }

    public boolean existsById(Integer id) {
        return customerRepo.existsById(id);
    }

    public Customer create(@NotBlank(message = "error.customer.nameRequired") String name,
                           @NotBlank(message = "error.customer.phoneRequired") String phone,
                           @Email(message = "error.invalidEmail") String email) {

        return customerRepo.save(new Customer(name.trim(), phone.trim(), normalizeEmail(email)));
    }

    public Customer update(@NotNull @Valid Customer customer) {
        requireExists(customer.getId());

        customer.setName(customer.getName().trim());
        customer.setPhone(customer.getPhone().trim());
        customer.setEmail(normalizeEmail(customer.getEmail()));

        return customerRepo.save(customer);
    }

    public void deleteById(Integer id) {
        requireExists(id);
        customerRepo.deleteById(id);
    }

    private void requireExists(int id) {
        if (!customerRepo.existsById(id)) {
            throw new EntityNotFoundException("Customer", id);
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim();
    }
}