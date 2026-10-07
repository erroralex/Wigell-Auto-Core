package com.wac.autocore.model;

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

    public Integer getOriginalWorkOrderId() {
        return originalWorkOrderId;
    }
}
