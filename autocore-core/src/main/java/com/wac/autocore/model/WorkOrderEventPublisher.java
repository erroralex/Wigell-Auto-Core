package com.wac.autocore.model;

@FunctionalInterface
public interface WorkOrderEventPublisher {
    WorkOrderEventPublisher NO_OP = event -> {};

    void publish(WorkOrderEvent event);
}
