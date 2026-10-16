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
 * <p>Ansvar: Bevisar att {@link ServiceItemService#updatePrice(int, Money)} samordnar rätt:
 * okänt id och ogiltigt pris sparar ingenting, ett giltigt pris sparas. Själva prisregeln testas i ServiceItemTest.</p>
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
    @DisplayName("updatePrice med okänt id kastar och sparar ingenting")
    void updatePriceUnknownIdThrowsEntityNotFoundException() {
        when(serviceItemRepo.findById(99)).thenReturn(Optional.empty());

        EntityNotFoundException e = assertThrows(EntityNotFoundException.class,
                () -> serviceItemService.updatePrice(99, Money.of(500)));

        assertEquals("error.serviceItem.notFound", e.getMessageKey());
        verify(serviceItemRepo, never()).save(any());
    }

    @Test
    @DisplayName("updatePrice med ogiltigt pris kastar, sparar ingenting och behåller priset")
    void updatePriceInvalidPriceThrowsValidationException() {
        ServiceItem item = new ServiceItem("Oljebyte", "beskrivning", Money.of("899.50"), 45);
        when(serviceItemRepo.findById(1)).thenReturn(Optional.of(item));

        ValidationException e = assertThrows(ValidationException.class,
                () -> serviceItemService.updatePrice(1, Money.ZERO));

        assertEquals("error.serviceItem.invalidPrice", e.getMessageKey());
        verify(serviceItemRepo, never()).save(any());
        assertMoney(Money.of("899.50"), item.getPrice());
    }

    @Test
    @DisplayName("updatePrice med giltigt pris ändrar och sparar")
    void updatePriceValidPriceUpdatesPrice() {
        ServiceItem item = new ServiceItem("Oljebyte", "beskrivning", Money.of("899.50"), 45);
        when(serviceItemRepo.findById(1)).thenReturn(Optional.of(item));
        when(serviceItemRepo.save(item)).thenReturn(item);

        ServiceItem result = serviceItemService.updatePrice(1, Money.of("999.00"));

        assertMoney(Money.of("999.00"), result.getPrice());
        verify(serviceItemRepo).save(item);
    }
}
