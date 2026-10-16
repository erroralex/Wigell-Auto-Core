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
 * <p>Ansvar: Bevisar att {@link CustomerService} inte accepterar påhittade kund-id:
 * update och deleteById kastar för okänt id och rör inte databasen.</p>
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
    @DisplayName("update med okänt id kastar och sparar ingenting")
    void updateUnknownIdThrowsEntityNotFoundException() {
        Customer customer = new Customer("Anna", "070-1234567", "anna@example.com");
        when(customerRepo.existsById(customer.getId())).thenReturn(false);

        EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
                () -> customerService.update(customer));

        assertEquals("error.notFound", e.getMessageKey());
        verify(customerRepo, never()).save(any());
    }

    @Test
    @DisplayName("update med känt id sparar kunden")
    void updateKnownIdSavesCustomer() {
        Customer customer = new Customer("Anna", "070-1234567", "anna@example.com");
        when(customerRepo.existsById(customer.getId())).thenReturn(true);
        when(customerRepo.save(customer)).thenReturn(customer);

        Customer result = customerService.update(customer);

        assertSame(customer, result);
        verify(customerRepo).save(customer);
    }

    @Test
    @DisplayName("deleteById med okänt id kastar och tar inte bort något")
    void deleteByIdUnknownIdThrowsEntityNotFoundException() {
        when(customerRepo.existsById(99)).thenReturn(false);

        EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
                () -> customerService.deleteById(99));

        assertEquals("error.notFound", e.getMessageKey());
        verify(customerRepo, never()).deleteById(any());
    }

    @Test
    @DisplayName("deleteById med känt id tar bort kunden")
    void deleteByIdKnownIdDeletesCustomer() {
        when(customerRepo.existsById(1)).thenReturn(true);

        customerService.deleteById(1);

        verify(customerRepo).deleteById(1);
    }
}
