package com.wac.autocore.unit;

import com.wac.autocore.exception.PricingIntegrityException;
import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.WorkOrderItem;
import org.junit.jupiter.api.Test;

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
        assertEquals(899.0, item.getAgreedPrice());
        assertEquals(45, item.getDurationMinutes());
        assertTrue(item.isChargeable());
    }

    private ServiceItem validService() {
        ServiceItem service = mock(ServiceItem.class);

        when(service.getId()).thenReturn(1);
        when(service.getName()).thenReturn("Oil Change");
        when(service.getPrice()).thenReturn(899.0);
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
        when(service.getPrice()).thenReturn(-1.0);

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
