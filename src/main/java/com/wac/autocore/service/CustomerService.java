package com.wac.autocore.service;

import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.model.Customer;
import com.wac.autocore.repository.CustomerRepo;
import org.springframework.format.annotation.NumberFormat;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.wac.autocore.exception.ValidationException;

import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Validated
@Service
public class CustomerService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
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

    public Customer create(@NotBlank(message = "error.requiredField") String name,
                           @NotBlank(message = "error.requiredField") String phone,
                           @Email(message = "error.invalidEmail") String email) {

        return customerRepo.save(new Customer(name.trim(), phone.trim(), normalizeEmail(email)));
    }

    public Customer update(@NotNull @Valid Customer customer) {
        if (customer == null || customer.getId() <= 0 || !customerRepo.existsById(customer.getId())) {
            throw new EntityNotFoundException("Customer", customer == null ? null : customer.getId());
        }

        customer.setName(customer.getName().trim());
        customer.setPhone(customer.getPhone().trim());
        customer.setEmail(normalizeEmail(customer.getEmail()));

        return customerRepo.save(customer);
    }

    public void deleteById(Integer id) {
        customerRepo.deleteById(id);
    }
    
    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim();
    }
}