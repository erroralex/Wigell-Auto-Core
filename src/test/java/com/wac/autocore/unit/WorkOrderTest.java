package com.wac.autocore.unit;

import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.exception.WorkOrderLockedException;
import com.wac.autocore.model.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        WarrantyWorkOrder warranty = warrantyDraft();
        warranty.addItem(createService(1, 899.0));
        warranty.addItem(createService(2, 499.0));

        warranty.setItemChargeable(2, false);

        assertEquals(899.0, warranty.getTotalPrice(), 0.001);
    }

    @Test
    void warrantyTotalIsZero_WhenAllItemsAreNonChargeable() {
        WarrantyWorkOrder warranty = warrantyDraft();
        warranty.addItem(createService(1, 899.0));

        warranty.setItemChargeable(1, false);

        assertEquals(0.0, warranty.getTotalPrice(), 0.001);
    }

    @Test
    void setItemChargeableThrows_WhenTypeDoesNotAllowNonChargeableItems() {
        TestWorkOrder workOrder = new TestWorkOrder(1, 1);
        workOrder.addItem(createService(1, 899.0));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> workOrder.setItemChargeable(1, false)
        );

        assertEquals("Validation failed: " + "error.workOrder.nonChargeableNotAllowed", exception.getMessage());
        assertTrue(findItem(workOrder, 1).isChargeable());
    }

    @Test
    void setItemChargeableAllowsNonChargeable_OnWarranty() {
        WarrantyWorkOrder warranty = warrantyDraft();
        warranty.addItem(createService(1, 899.0));

        warranty.setItemChargeable(1, false);

        assertFalse(findItem(warranty, 1).isChargeable());
    }

    @Test
    void applyChargeFlagsIsIgnored_WhenTypeDoesNotAllowNonChargeableItems() {
        TestWorkOrder workOrder = new TestWorkOrder(1, 1);
        workOrder.addItem(createService(1, 899.0));

        workOrder.applyChargeFlags(Collections.singletonMap(1, false));

        assertTrue(findItem(workOrder, 1).isChargeable());
    }

    @Test
    void applyChargeFlagsSetsFlags_AndMissingItemsBecomeNonChargeable() {
        WarrantyWorkOrder warranty = warrantyDraft();
        warranty.addItem(createService(1, 899.0));
        warranty.addItem(createService(2, 499.0));

        Map<Integer, Boolean> flags = new HashMap<>();
        flags.put(1, true);

        warranty.applyChargeFlags(flags);

        assertTrue(findItem(warranty, 1).isChargeable());
        assertFalse(findItem(warranty, 2).isChargeable());
    }

    @Test
    void originalWorkOrderIdIsNull_ForOrdersWithoutOriginal() {
        TestWorkOrder workOrder = new TestWorkOrder(1, 1);

        assertNull(workOrder.getOriginalWorkOrderId());
    }

    @Test
    void originalWorkOrderIdIsReturned_ForWarranty() {
        WarrantyWorkOrder warranty = warrantyDraft();

        assertEquals(Integer.valueOf(7), warranty.getOriginalWorkOrderId());
    }

    private ServiceItem createService(int id, double price) {
        ServiceItem service = mock(ServiceItem.class);

        when(service.getId()).thenReturn(id);
        when(service.getName()).thenReturn("Oljebyte");
        when(service.getPrice()).thenReturn(price);
        when(service.getEstimatedMinutes()).thenReturn(45);

        return service;
    }

    // Ett avslutat original som en reklamation kan skapas från. Mockas eftersom id sätts av databasen.
    private WorkOrder completedOriginal() {
        WorkOrder original = mock(WorkOrder.class);
        when(original.getId()).thenReturn(7);
        when(original.getStatus()).thenReturn(WorkOrderStatus.COMPLETED);
        when(original.getVehicleId()).thenReturn(1);
        return original;
    }

    private WarrantyWorkOrder warrantyDraft() {
        return WarrantyWorkOrder.draft(completedOriginal(), "Läcker igen");
    }

    private WorkOrderItem findItem(WorkOrder workOrder, int serviceItemId) {
        return workOrder.getItems().stream()
                .filter(item -> item.getServiceItemId() == serviceItemId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No item with id " + serviceItemId));
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
