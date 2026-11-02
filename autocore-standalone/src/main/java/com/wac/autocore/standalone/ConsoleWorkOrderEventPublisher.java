package com.wac.autocore.standalone;

import com.wac.autocore.model.WorkOrderEvent;
import com.wac.autocore.model.WorkOrderEventPublisher;

import java.io.PrintStream;
import java.util.Objects;

/**
 * <b>ConsoleWorkOrderEventPublisher</b>
 * <p>Responsibility: Prints each {@link WorkOrderEvent} as one indented line on the console,
 * so the standalone demo shows the work order lifecycle as it happens.</p>
 *
 * <p>Acts as a <b>concrete observer</b> in the <b>Observer</b> pattern (GoF):
 * {@code WorkOrderService} is the subject and notifies it through {@link WorkOrderEventPublisher}.
 * In {@code autocore-app} a Spring adapter fills the same role.</p>
 */
public class ConsoleWorkOrderEventPublisher implements WorkOrderEventPublisher {

    private final PrintStream out;

    public ConsoleWorkOrderEventPublisher(PrintStream out) {
        this.out = Objects.requireNonNull(out, "out");
    }

    @Override
    public void publish(WorkOrderEvent event) {
        out.println("  [event] " + event.action() + " " + event.workOrderType() + " #" + event.workOrderId());
    }
}
