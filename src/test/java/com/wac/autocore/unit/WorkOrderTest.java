package com.wac.autocore.unit;

import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.exception.WorkOrderLockedException;
import com.wac.autocore.model.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WorkOrderTest {

    @Test
    void addItemAddsServiceSnapshotToDraft() {
        TestWorkOrder workOrder = new TestWorkOrder(1, 1);
        ServiceItem service = createService(1, 899.0);

        workOrder.addItem(service);

        assertEquals(1, workOrder.getItems().size());

        WorkOrderItem item = workOrder.getItems().get(0);
        assertEquals(1, item.getServiceItemId());
        assertEquals("Oljebyte", item.getServiceName());
        assertEquals(899.0, item.getAgreedPrice(), 0.001);
        assertEquals(45, item.getDurationMinutes());
    }

    @Test
    void addedItemPriceDoesNotChange_WhenCatalogPriceChanges() {
        TestWorkOrder workOrder = new TestWorkOrder(1, 1);
        ServiceItem service = createService(1, 899.0);

        workOrder.addItem(service);

        when(service.getPrice()).thenReturn(999.0);

        assertEquals(899.0, workOrder.getItems().get(0).getAgreedPrice(), 0.001);
    }

    @Test
    void addItemThrows_WhenWorkOrderIsInProgress() {
        TestWorkOrder workOrder = new TestWorkOrder(1, 1);
        workOrder.addItem(createService(1, 899.0));

        workOrder.confirm();
        workOrder.start();

        assertEquals(WorkOrderStatus.IN_PROGRESS, workOrder.getStatus());

        assertThrows(
                WorkOrderLockedException.class,
                () -> workOrder.addItem(createService(2, 499.0))
        );
    }

    @Test
    void addItemThrowsWhenServiceAlreadyExists() {
        TestWorkOrder workOrder = new TestWorkOrder(1, 1);

        workOrder.addItem(createService(1, 899.0));

        assertThrows(
                ValidationException.class,
                () -> workOrder.addItem(createService(1, 999.0))
        );

        assertEquals(1, workOrder.getItems().size());
    }

    @Test
    void removeItemCannotRemoveLastService() {
        TestWorkOrder workOrder = new TestWorkOrder(1, 1);
        workOrder.addItem(createService(1, 899.0));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> workOrder.removeItem(1)
        );

        assertEquals("Validation failed: " + "error.workOrder.missingServices", exception.getMessage());
        assertEquals(1, workOrder.getItems().size());
    }

    @Test
    void totalPriceCountsOnlyChargeableItems() {
        TestWorkOrder workOrder = new TestWorkOrder(1, 1);
        workOrder.addItem(createService(1, 899.0));
        workOrder.addItem(createService(2, 499.0));

        workOrder.setItemChargeable(2, false);

        assertEquals(899.0, workOrder.getTotalPrice(), 0.001);
    }

    @Test
    void warrantyTotalIsZero_WhenAllItemsAreNonChargeable() {
        WorkOrder original = mock(WorkOrder.class);
        when(original.getId()).thenReturn(7);
        when(original.getStatus()).thenReturn(WorkOrderStatus.COMPLETED);
        when(original.getVehicleId()).thenReturn(1);

        WarrantyWorkOrder warranty = WarrantyWorkOrder.draft(original, "Läcker igen");
        warranty.addItem(createService(1, 899.0));

        warranty.setItemChargeable(1, false);

        assertEquals(0.0, warranty.getTotalPrice(), 0.001);
    }

    private ServiceItem createService(int id, double price) {
        ServiceItem service = mock(ServiceItem.class);

        when(service.getId()).thenReturn(id);
        when(service.getName()).thenReturn("Oljebyte");
        when(service.getPrice()).thenReturn(price);
        when(service.getEstimatedMinutes()).thenReturn(45);

        return service;
    }

    private static class TestWorkOrder extends WorkOrder {

        TestWorkOrder(Integer vehicleId, Integer mechanicId) {
            super(null, vehicleId, mechanicId, new ArrayList<>());
        }

        @Override
        public WorkOrderType getType() {
            return WorkOrderType.PLANNED;
        }
    }
}
