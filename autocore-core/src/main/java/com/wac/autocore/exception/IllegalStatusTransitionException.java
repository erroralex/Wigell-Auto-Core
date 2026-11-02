package com.wac.autocore.exception;

import com.wac.autocore.model.WorkOrderStatus;

public class IllegalStatusTransitionException extends DomainException {

    public IllegalStatusTransitionException(WorkOrderStatus from, WorkOrderStatus to) {
        super(
                "Illegal status transition: " + from + " -> " + to,
                "error.statusTransition",
                "error.statusTransitionMsg",
                from,
                to
        );
    }
}