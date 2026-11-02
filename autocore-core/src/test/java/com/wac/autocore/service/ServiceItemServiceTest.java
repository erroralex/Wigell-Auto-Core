package com.wac.autocore.service;

import com.wac.autocore.exception.EntityNotFoundException;
import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.value.Money;
import com.wac.autocore.repository.ServiceItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.wac.autocore.testsupport.MoneyAssert.assertMoney;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * <b>ServiceItemServiceTest</b>
 * <p>Responsibility: Proves that {@link ServiceItemService#updatePrice(int, Money)} coordinates correctly:
 * an unknown id and an invalid price save nothing, a valid price is saved. The price rule itself is tested in ServiceItemTest.</p>
 */
class ServiceItemServiceTest {

    private ServiceItemRepository serviceItemRepo;
    private ServiceItemService serviceItemService;

    @BeforeEach
    void setUp() {
        serviceItemRepo = mock(ServiceItemRepository.class);
        serviceItemService = new ServiceItemService(serviceItemRepo);
    }

    @Test
    @DisplayName("updatePrice with an unknown id throws and saves nothing")
    void updatePriceUnknownIdThrowsEntityNotFoundException() {
        when(serviceItemRepo.findById(99)).thenReturn(Optional.empty());

        EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
                () -> serviceItemService.updatePrice(99, Money.of(500)));

        assertEquals("error.serviceItem.notFound", e.getMessageKey());
        verify(serviceItemRepo, never()).save(any());
    }

    @Test
    @DisplayName("updatePrice with an invalid price throws, saves nothing and keeps the price")
    void updatePriceInvalidPriceThrowsValidationException() {
        ServiceItem item = new ServiceItem("Oil change", "description", Money.of("899.50"), 45);
        when(serviceItemRepo.findById(1)).thenReturn(Optional.of(item));

        ValidationException e = assertThrows(ValidationException.class,
                () -> serviceItemService.updatePrice(1, Money.ZERO));

        assertEquals("error.serviceItem.invalidPrice", e.getMessageKey());
        verify(serviceItemRepo, never()).save(any());
        assertMoney(Money.of("899.50"), item.getPrice());
    }

    @Test
    @DisplayName("updatePrice with a valid price changes and saves")
    void updatePriceValidPriceUpdatesPrice() {
        ServiceItem item = new ServiceItem("Oil change", "description", Money.of("899.50"), 45);
        when(serviceItemRepo.findById(1)).thenReturn(Optional.of(item));
        when(serviceItemRepo.save(item)).thenReturn(item);

        ServiceItem result = serviceItemService.updatePrice(1, Money.of("999.00"));

        assertMoney(Money.of("999.00"), result.getPrice());
        verify(serviceItemRepo).save(item);
    }
}
