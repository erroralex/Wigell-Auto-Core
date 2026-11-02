package com.wac.autocore.unit;

import com.wac.autocore.exception.IllegalStatusTransitionException;
import com.wac.autocore.exception.ValidationException;
import com.wac.autocore.exception.WorkOrderLockedException;
import com.wac.autocore.model.DropInWorkOrder;
import com.wac.autocore.model.ServiceItem;
import com.wac.autocore.model.WarrantyWorkOrder;
import com.wac.autocore.model.WorkOrder;
import com.wac.autocore.model.WorkOrderItem;
import com.wac.autocore.model.WorkOrderStatus;
import com.wac.autocore.model.WorkOrderType;
import com.wac.autocore.model.value.Money;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.wac.autocore.testsupport.MoneyAssert.assertMoney;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WorkOrderTest {

    @Test
    void addItemAddsServiceSnapshotToDraft() {
        TestWorkOrder workOrder = new TestWorkOrder(1, 1);
        ServiceItem service = createService(1, Money.of(899));

        workOrder.addItem(service);

        assertEquals(1, workOrder.getItems().size());

        WorkOrderItem item = workOrder.getItems().get(0);
        assertEquals(1, item.getServiceItemId());
        assertEquals("Oil change", item.getServiceName());
        assertMoney(Money.of(899), item.getAgreedPrice());
        assertEquals(45, item.getDurationMinutes());
    }

    @Test
    void addedItemPriceDoesNotChange_WhenCatalogPriceChanges() {
        TestWorkOrder workOrder = new TestWorkOrder(1, 1);
        ServiceItem service = createService(1, Money.of(899));

        workOrder.addItem(service);

        when(service.getPrice()).thenReturn(Money.of(999));

        assertMoney(Money.of(899), workOrder.getItems().get(0).getAgreedPrice());
    }

    @Test
    void addItemThrows_WhenWorkOrderIsInProgress() {
        TestWorkOrder workOrder = new TestWorkOrder(1, 1);
        workOrder.addItem(createService(1, Money.of(899)));

        workOrder.confirm();
        workOrder.start();

        assertEquals(WorkOrderStatus.IN_PROGRESS, workOrder.getStatus());

        assertThrows(
                WorkOrderLockedException.class,
                () -> workOrder.addItem(createService(2, Money.of(499)))
        );
    }

    @Test
    void addItemThrowsWhenServiceAlreadyExists() {
        TestWorkOrder workOrder = new TestWorkOrder(1, 1);

        workOrder.addItem(createService(1, Money.of(899)));

        assertThrows(
                ValidationException.class,
                () -> workOrder.addItem(createService(1, Money.of(999)))
        );

        assertEquals(1, workOrder.getItems().size());
    }

    @Test
    void replaceItemsCannotRemoveAllServices() {
        TestWorkOrder workOrder = new TestWorkOrder(1, 1);
        workOrder.addItem(createService(1, Money.of(899)));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> workOrder.replaceItems(Collections.<ServiceItem>emptyList())
        );

        assertEquals("Validation failed: " + "error.workOrder.missingServices", exception.getMessage());
        assertEquals(1, workOrder.getItems().size());
    }

    @Test
    void totalPriceCountsOnlyChargeableItems() {
        WarrantyWorkOrder warranty = warrantyDraft();
        warranty.addItem(createService(1, Money.of(899)));
        warranty.addItem(createService(2, Money.of(499)));

        warranty.setItemChargeable(2, false);

        assertMoney(Money.of(899), warranty.getTotalPrice());
    }

    @Test
    void warrantyTotalIsZero_WhenAllItemsAreNonChargeable() {
        WarrantyWorkOrder warranty = warrantyDraft();
        warranty.addItem(createService(1, Money.of(899)));

        warranty.setItemChargeable(1, false);

        assertMoney(Money.ZERO, warranty.getTotalPrice());
    }

    @Test
    void setItemChargeableThrows_WhenTypeDoesNotAllowNonChargeableItems() {
        TestWorkOrder workOrder = new TestWorkOrder(1, 1);
        workOrder.addItem(createService(1, Money.of(899)));

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
        warranty.addItem(createService(1, Money.of(899)));

        warranty.setItemChargeable(1, false);

        assertFalse(findItem(warranty, 1).isChargeable());
    }

    @Test
    void applyChargeFlagsIsIgnored_WhenTypeDoesNotAllowNonChargeableItems() {
        TestWorkOrder workOrder = new TestWorkOrder(1, 1);
        workOrder.addItem(createService(1, Money.of(899)));

        workOrder.applyChargeFlags(Collections.singletonMap(1, false));

        assertTrue(findItem(workOrder, 1).isChargeable());
    }

    @Test
    void applyChargeFlagsSetsFlags_AndMissingItemsBecomeNonChargeable() {
        WarrantyWorkOrder warranty = warrantyDraft();
        warranty.addItem(createService(1, Money.of(899)));
        warranty.addItem(createService(2, Money.of(499)));

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

    @Test
    void dropInDraftThrows_WhenProblemDescriptionIsBlank() {
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> DropInWorkOrder.draft(1, "   ")
        );

        assertEquals("Validation failed: " + "error.workOrder.missingProblemDescription", exception.getMessage());
    }

    @Test
    void warrantyDraftThrows_WhenProblemDescriptionIsMissing() {
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> WarrantyWorkOrder.draft(completedOriginal(), null)
        );

        assertEquals("Validation failed: " + "error.workOrder.missingProblemDescription", exception.getMessage());
    }

    @Test
    void startThrows_WhenConfirmedOrderHasLostItsMechanic() {
        TestWorkOrder workOrder = new TestWorkOrder(1, 1);
        workOrder.addItem(createService(1, Money.of(899)));
        workOrder.confirm();

        workOrder.setMechanicId(null);

        assertThrows(ValidationException.class, workOrder::start);
        assertEquals(WorkOrderStatus.CONFIRMED, workOrder.getStatus());
    }

    @Test
    void requireCompleteIfConfirmed_allowsIncompleteDraft() {
        TestWorkOrder draft = new TestWorkOrder(1, null);

        draft.requireCompleteIfConfirmed(); // should not throw

        assertEquals(WorkOrderStatus.DRAFT, draft.getStatus());
    }

    @Test
    void requireCompleteIfConfirmed_throwsForIncompleteConfirmedOrder() {
        TestWorkOrder workOrder = new TestWorkOrder(1, 1);
        workOrder.addItem(createService(1, Money.of(899)));
        workOrder.confirm();
        workOrder.setMechanicId(null);

        assertThrows(ValidationException.class, workOrder::requireCompleteIfConfirmed);
    }

    @Test
    void startThrowsStatusTransition_beforeValidation_WhenOrderIsCancelled() {
        TestWorkOrder workOrder = new TestWorkOrder(1, null); // incomplete draft
        workOrder.cancel();

        assertThrows(IllegalStatusTransitionException.class, workOrder::start);
    }

    private ServiceItem createService(int id, Money price) {
        ServiceItem service = mock(ServiceItem.class);

        when(service.getId()).thenReturn(id);
        when(service.getName()).thenReturn("Oil change");
        when(service.getPrice()).thenReturn(price);
        when(service.getEstimatedMinutes()).thenReturn(45);

        return service;
    }

    // A completed original that a warranty claim can be created from. Mocked because the id is set by the database.
    private WorkOrder completedOriginal() {
        WorkOrder original = mock(WorkOrder.class);
        when(original.getId()).thenReturn(7);
        when(original.getStatus()).thenReturn(WorkOrderStatus.COMPLETED);
        when(original.getVehicleId()).thenReturn(1);
        return original;
    }

    private WarrantyWorkOrder warrantyDraft() {
        return WarrantyWorkOrder.draft(completedOriginal(), "Leaking again");
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
