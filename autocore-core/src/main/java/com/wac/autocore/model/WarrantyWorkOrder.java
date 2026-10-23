package com.wac.autocore.model;

import com.wac.autocore.exception.ValidationException;

import java.util.Collections;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("WARRANTY")
public class WarrantyWorkOrder extends WorkOrder {

    @Column(name = "original_work_order_id")
    private Integer originalWorkOrderId;

    protected WarrantyWorkOrder() {
    }

    // Creates a draft of a warranty order based on an existing order.
    // The new order gets the same vehicle as the original and the problem is described by the user.
    private WarrantyWorkOrder(WorkOrder original, String problemDescription) {
        super(
                null,
                original.getVehicleId(),
                null,
                Collections.emptyList()
        );

        this.originalWorkOrderId = original.getId();
        setProblemDescription(problemDescription);

    }

    // Creates a draft of a warranty order. Validates that the original is valid and that the problem has a description.
    public static WarrantyWorkOrder draft(WorkOrder original, String problemDescription) {

        if (original == null || original.getId() <= 0) {
            throw new ValidationException("error.workOrder.missingOriginal");
        }

        if (original.getStatus() != WorkOrderStatus.COMPLETED) {
            throw new ValidationException("error.workOrder.originalNotCompleted");
        }

        if (original.getVehicleId() == null || original.getVehicleId() <= 0) {
            throw new ValidationException("error.workOrder.missingVehicle");
        }

        requireProblemDescription(WorkOrderType.WARRANTY, problemDescription);

        return new WarrantyWorkOrder(original, problemDescription);

    }

    @Override
    public WorkOrderType getType() {
        return WorkOrderType.WARRANTY;
    }

    // Validates that the original order id is set and valid. Called by confirm() in the base class.
    @Override
    protected void validateTypeSpecific() {
        if (originalWorkOrderId == null || originalWorkOrderId <= 0) {
            throw new ValidationException("error.workOrder.missingOriginal");
        }
    }

    @Override
    public boolean allowsNonChargeableItems() {
        return true;
    }

    @Override
    public Integer getOriginalWorkOrderId() {
        return originalWorkOrderId;
    }
}
