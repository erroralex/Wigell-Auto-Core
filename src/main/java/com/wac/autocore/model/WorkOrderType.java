package com.wac.autocore.model;

// Representerar typen av arbetsorder. Språknyckeln används av vyn för översättning, så vyn behöver inga egna typregler.
// (Förslag: dialogen har exakt samma tre värden som WorkOrderType,
// så det vore bättre att refaktorera och använda samma enum i dialogen också.
// Ger en enda källa till sanning, och ingen duplicering.)
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