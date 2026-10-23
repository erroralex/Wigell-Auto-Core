package com.wac.autocore.service;

import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.model.Customer;
import com.wac.autocore.repository.CustomerRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * <b>CustomerServiceTest</b>
 * <p>Responsibility: Proves that {@link CustomerService} does not accept made-up customer ids:
 * update and deleteById throw for an unknown id and do not touch the database.</p>
 */
class CustomerServiceTest {

    private CustomerRepo customerRepo;
    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerRepo = mock(CustomerRepo.class);
        customerService = new CustomerService(customerRepo);
    }

    @Test
    @DisplayName("update with an unknown id throws and saves nothing")
    void updateUnknownIdThrowsEntityNotFoundException() {
        Customer customer = new Customer("Anna", "070-1234567", "anna@example.com");
        when(customerRepo.existsById(customer.getId())).thenReturn(false);

        EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
                () -> customerService.update(customer));

        assertEquals("error.notFound", e.getMessageKey());
        verify(customerRepo, never()).save(any());
    }

    @Test
    @DisplayName("update with a known id saves the customer")
    void updateKnownIdSavesCustomer() {
        Customer customer = new Customer("Anna", "070-1234567", "anna@example.com");
        when(customerRepo.existsById(customer.getId())).thenReturn(true);
        when(customerRepo.save(customer)).thenReturn(customer);

        Customer result = customerService.update(customer);

        assertSame(customer, result);
        verify(customerRepo).save(customer);
    }

    @Test
    @DisplayName("deleteById with an unknown id throws and deletes nothing")
    void deleteByIdUnknownIdThrowsEntityNotFoundException() {
        when(customerRepo.existsById(99)).thenReturn(false);

        EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
                () -> customerService.deleteById(99));

        assertEquals("error.notFound", e.getMessageKey());
        verify(customerRepo, never()).deleteById(any());
    }

    @Test
    @DisplayName("deleteById with a known id deletes the customer")
    void deleteByIdKnownIdDeletesCustomer() {
        when(customerRepo.existsById(1)).thenReturn(true);

        customerService.deleteById(1);

        verify(customerRepo).deleteById(1);
    }
}
