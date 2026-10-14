package com.wac.autocore.service;

import com.wac.autocore.model.Customer;
import com.wac.autocore.repository.CustomerRepo;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
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
                           @Email @NotBlank(message = "error.customer.emailInvalid") String email) {
        return customerRepo.save(new Customer(name, phone, email));
    }

    public Customer update(Customer customer) {
        return customerRepo.save(customer);
    }

    public void deleteById(Integer id) {
        customerRepo.deleteById(id);
    }

}