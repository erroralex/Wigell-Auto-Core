package com.wac.autocore.model;

// Represents the type of work order. The language key is used by the view for translation, so the view needs no type rules of its own.
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