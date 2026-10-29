package com.wac.autocore.model;

/**
 * Interface serving as the observer in the Observer Design Pattern
 * for {@link WorkOrderEvent} instances. {@code WorkOrderService} is the subject
 * and notifies it on every work order state change.
 */
@FunctionalInterface
public interface WorkOrderEventPublisher {

    // Publisher that intentionally ignores published events.
    WorkOrderEventPublisher NO_OP = event -> {};

    /**
     * Publishes a {@link WorkOrderEvent}
     *
     * @param event the event to publish
     */
    void publish(WorkOrderEvent event);
}
