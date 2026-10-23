package com.wac.autocore.headless;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * <b>ScenarioStep</b>
 * <p>Responsibility: A step in the headless scenario, implemented as a <b>Command</b>. Each step encapsulates
 * an action against the services, so {@link DropInScenario} can run, catch and report
 * all steps the same way without knowing what they do.</p>
 * <p>Steps are created with the factories {@link #step} (should succeed) and {@link #expectFailure}
 * (should fail with a business error), which take a lambda and wrap it in a command.</p>
 * <p><b>Example:</b></p>
 * <pre>{@code
 * step("Confirm order", ctx -> workOrderService.confirmWorkOrder(ctx.getWorkOrderId()))
 * }</pre>
 */
public interface ScenarioStep {

    String name();
    boolean expectsFailure();
    void execute(ScenarioContext ctx);

    // Creates a step that should fail with a business error
    static ScenarioStep expectFailure(String name, Consumer<ScenarioContext> action) {
        return create(name, true, action);
    }

    // Creates a step that should succeed
    static ScenarioStep step(String name, Consumer<ScenarioContext> action) {
        return create(name, false, action);
    }

    static ScenarioStep create(String name, boolean expectsFailure, Consumer<ScenarioContext> action) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(action, "action");

        return new ScenarioStep() {

            @Override
            public String name() {
                return name;
            }

            @Override
            public boolean expectsFailure() {
                return expectsFailure;
            }

            @Override
            public void execute(ScenarioContext ctx) {
                action.accept(ctx);
            }
        };
    }
}
