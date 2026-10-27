package com.wac.autocore.service;

import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.model.Customer;
import com.wac.autocore.repository.CustomerRepo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;


import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Optional;

@Validated
@Service
@PreAuthorize("@access.can('DATA_READ')") // Ensure that only users with 'DATA_READ' permission can access this service
public class CustomerService {

    private final CustomerRepo customerRepo;

    public CustomerService(CustomerRepo customerRepo) {
        this.customerRepo = customerRepo;
    }

    public List<Customer> findAll() {
        return customerRepo.findAll();
    }

    public Optional<Customer> findById(@Positive @NotNull(message="error.customerNull") Integer id) {
        return customerRepo.findById(id);
    }

    public boolean existsById(@Positive @NotNull(message="error.customerNull") Integer id) {
        return customerRepo.existsById(id);
    }

    @PreAuthorize("@access.can('CUSTOMER_WRITE')") // Ensure that only users with 'CUSTOMER_WRITE' permission can create customers
    public Customer create(@NotBlank(message = "error.customer.nameRequired") String name,
                           @NotBlank(message = "error.customer.phoneRequired") String phone,
                           @Email(message = "error.invalidEmail") String email) {

        return customerRepo.save(new Customer(name.trim(), phone.trim(), normalizeEmail(email)));
    }

    @PreAuthorize("@access.can('CUSTOMER_WRITE')")
    public Customer update(@NotNull @Valid Customer customer) {
        requireExists(customer.getId());

        customer.setName(customer.getName().trim());
        customer.setPhone(customer.getPhone().trim());
        customer.setEmail(normalizeEmail(customer.getEmail()));

        return customerRepo.save(customer);
    }

    @PreAuthorize("@access.can('CUSTOMER_WRITE')")
    public void deleteById(@Positive @NotNull(message="error.customerNull") Integer id) {
        requireExists(id);
        customerRepo.deleteById(id);
    }

    private void requireExists(Integer id) {
        if (!customerRepo.existsById(id)) {
            throw new EntityNotFoundException("Customer", id);
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim();
    }
}