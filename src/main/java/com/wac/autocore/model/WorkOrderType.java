package com.wac.autocore.model;

// Representerar typen av arbetsorder. Språknyckeln används av vyn för översättning, så vyn behöver inga egna typregler.
// (Förslag: dialogen har exakt samma tre värden som WorkOrderType,
// så det vore bättre att refaktorera och använda samma enum i dialogen också.
// Ger en enda källa till sanning, och ingen duplicering.)
public enum WorkOrderType {

    PLANNED("workOrder.type.planned"),
    DROP_IN("workOrder.type.dropIn"),
    WARRANTY("workOrder.type.warranty");

    private final String messageKey;

    WorkOrderType(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }

}