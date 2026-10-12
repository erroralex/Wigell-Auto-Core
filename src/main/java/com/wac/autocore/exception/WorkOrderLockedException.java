package com.wac.autocore.exception;

import com.wac.autocore.model.WorkOrderStatus;

public class WorkOrderLockedException extends DomainException {
    public WorkOrderLockedException(int workOrderId, WorkOrderStatus status) {
        super(
                "Work order " + workOrderId + " is locked in status " + status,
                "error.workOrderLocked",
                "error.workOrderLockedMsg",
                workOrderId, status
        );
    }
}