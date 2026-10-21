package com.wac.autocore.model;

// Representerar typen av arbetsorder. Språknyckeln används av vyn för översättning, så vyn behöver inga egna typregler.
public enum WorkOrderType {

    PLANNED("workOrder.type.planned", false),
    DROP_IN("workOrder.type.dropIn", true),
    WARRANTY("workOrder.type.warranty", true);

    private final String messageKey;

    private final boolean requiresProblemDescription;

    WorkOrderType(String messageKey, boolean requiresProblemDescription) {
        this.messageKey = messageKey;
        this.requiresProblemDescription = requiresProblemDescription;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public boolean requiresProblemDescription() {
        return requiresProblemDescription;
    }

}