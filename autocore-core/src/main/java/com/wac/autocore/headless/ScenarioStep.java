package com.wac.autocore.headless;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * <b>ScenarioStep</b>
 * <p>Ansvar: Ett steg i headless-scenariot, implementerat som <b>Command</b>. Varje steg kapslar
 * in en handling mot tjänsterna, så att {@link DropInScenario} kan köra, fånga och rapportera
 * alla steg på samma sätt utan att veta vad de gör.</p>
 * <p>Steg skapas med fabrikerna {@link #step} (ska lyckas) och {@link #expectFailure}
 * (ska misslyckas med ett affärsfel), som tar en lambda och slår in den i ett kommando.</p>
 * <p><b>Exempel:</b></p>
 * <pre>{@code
 * step("Bekräfta order", ctx -> workOrderService.confirmWorkOrder(ctx.getWorkOrderId()))
 * }</pre>
 */
public interface ScenarioStep {

    String name();
    boolean expectsFailure();
    void execute(ScenarioContext ctx);

    // Skapar ett steg som ska misslyckas med ett affärsfel
    static ScenarioStep expectFailure(String name, Consumer<ScenarioContext> action) {
        return create(name, true, action);
    }

    // Skapar ett steg som ska lyckas med ett affärsfel
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
