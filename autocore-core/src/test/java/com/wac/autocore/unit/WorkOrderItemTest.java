package com.wac.autocore.unit;

import com.wac.autocore.exception.PricingIntegrityException;
import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.WorkOrderItem;
import com.wac.autocore.model.value.Money;
import org.junit.jupiter.api.Test;

import static com.wac.autocore.testsupport.MoneyAssert.assertMoney;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WorkOrderItemTest {

    @Test
    void snapshotOf_CopiesServiceDate() {
        ServiceItem service = validService();

        WorkOrderItem item = WorkOrderItem.snapshotOf(service);

        assertEquals(1, item.getServiceItemId());
        assertEquals("Oil Change", item.getServiceName());
        assertMoney(Money.of(899), item.getAgreedPrice());
        assertEquals(45, item.getDurationMinutes());
        assertTrue(item.isChargeable());
    }

    @Test
    void chargedAmountEqualsAgreedPrice_WhenChargeable() {
        WorkOrderItem item = WorkOrderItem.snapshotOf(validService());

        assertMoney(Money.of(899), item.getChargedAmount());
    }

    @Test
    void chargedAmountIsZero_WhenNotChargeable() {
        WorkOrderItem item = WorkOrderItem.snapshotOf(validService());

        item.setChargeable(false);

        assertMoney(Money.ZERO, item.getChargedAmount());
        assertMoney(Money.of(899), item.getAgreedPrice());
    }

    @Test
    void equalsAndHashCodeIgnoreHowThePriceWasWritten() {
        WorkOrderItem a = itemWithPrice(Money.of("899"));
        WorkOrderItem b = itemWithPrice(Money.of("899.00"));

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, itemWithPrice(Money.of("899.01")));
    }

    private WorkOrderItem itemWithPrice(Money price) {
        ServiceItem service = validService();
        when(service.getPrice()).thenReturn(price);
        return WorkOrderItem.snapshotOf(service);
    }

    private ServiceItem validService() {
        ServiceItem service = mock(ServiceItem.class);

        when(service.getId()).thenReturn(1);
        when(service.getName()).thenReturn("Oil Change");
        when(service.getPrice()).thenReturn(Money.of(899));
        when(service.getEstimatedMinutes()).thenReturn(45);

        return service;
    }

    @Test
    void snapshotOfThrowsValidationException_WhenServiceIsNull() {
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> WorkOrderItem.snapshotOf(null)
        );

        assertEquals("Validation failed: " + "error.serviceSelect", exception.getMessage());
    }

    @Test
    void snapshotOfThrows_WhenServiceIsNotSaved() {
        ServiceItem service = validService();
        when(service.getId()).thenReturn(0);

        PricingIntegrityException exception = assertThrows(
                PricingIntegrityException.class,
                () -> WorkOrderItem.snapshotOf(service)
        );

        assertEquals("Service item is not saved", exception.getMessage());
    }

    @Test
    void snapshotOfThrows_WhenPriceIsNegative() {
        ServiceItem service = validService();
        when(service.getPrice()).thenReturn(Money.of(-1));

        PricingIntegrityException exception = assertThrows(
                PricingIntegrityException.class,
                () -> WorkOrderItem.snapshotOf(service)
        );

        assertEquals(
                "Service item 1 has invalid catalog data",
                exception.getMessage()
        );
    }
}
