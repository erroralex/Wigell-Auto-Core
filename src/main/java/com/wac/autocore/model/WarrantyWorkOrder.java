package com.wac.autocore.model;

import com.wac.autocore.exception.ValidationException;

import java.util.Collections;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("WARRANTY")
public class WarrantyWorkOrder extends WorkOrder {

    @Column(name = "original_work_order_id")
    private Integer originalWorkOrderId;

    protected WarrantyWorkOrder() {
    }

    // Skapar ett utkast till en garantiorder baserat på en befintlig order.
    // Den nya ordern får samma fordon som originalet och problem beskrivs av användaren.
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

    // Skapar ett utkast till en garantiorder. Validerar att originalet är giltigt och att problemet har en beskrivning.
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

        if (problemDescription == null || problemDescription.trim().isEmpty()) {
            throw new ValidationException("error.workOrder.missingProblemDescription");
        }

        return new WarrantyWorkOrder(original, problemDescription);

    }

    // Validerar att originalorder-id är satt och giltigt. Anropas av confirm() i bas-klassen.
    @Override
    protected void validateTypeSpecific() {
        if (originalWorkOrderId == null || originalWorkOrderId <= 0) {
            throw new ValidationException("error.workOrder.missingOriginal");
        }
    }

    public Integer getOriginalWorkOrderId() {
        return originalWorkOrderId;
    }

}
