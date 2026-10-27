package com.wac.autocore.model;

/**
 * Interface serving as the subject (publisher) in the Observer Design Pattern.
 * for {@link WorkOrderEvent} instances.
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
