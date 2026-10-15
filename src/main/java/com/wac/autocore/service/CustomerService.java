package com.wac.autocore.service;

import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.Customer;
import com.wac.autocore.repository.CustomerRepo;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;


import javax.validation.Valid;
import javax.validation.constraints.*;
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
                           @NotBlank(message = "error.customer.phoneRequired")
                           @Pattern(regexp = "^[0-9-]+$", message = "error.customer.phoneDigitsOnly")
                           String phone,
                           @Email(message = "error.invalidEmail") String email) {

        return customerRepo.save(new Customer(name.trim(), phone.trim(), normalizeEmail(email)));
    }

    public Customer update(@NotNull @Positive Integer customerId,
                            @NotBlank(message = "error.customer.nameRequired") String name,
                           @NotBlank(message = "error.customer.phoneRequired")
                           @Pattern(regexp = "^[0-9-]+$", message = "error.customer.phoneDigitsOnly")
                           String phone,
                           @Email(message = "error.invalidEmail") String email,
                            boolean isVip) {

        Customer existingCustomer = customerRepo.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("customer", customerId));

        existingCustomer.setName(name.trim());
        existingCustomer.setPhone(phone.trim());
        existingCustomer.setEmail(normalizeEmail(email));
        existingCustomer.setVip(isVip);

        return customerRepo.save(existingCustomer);
    }

    public void deleteById(Integer id) {
        customerRepo.deleteById(id);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim();
    }
}