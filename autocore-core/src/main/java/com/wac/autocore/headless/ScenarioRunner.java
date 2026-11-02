package com.wac.autocore.headless;

import com.wac.autocore.exception.handler.ErrorHandler;
import com.wac.autocore.exception.handler.ErrorHandlerChain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <b>ScenarioRunner</b>
 * <p>Responsibility: Runs scenario steps in order and turns each outcome into a {@link StepResult}.
 * Stops after a regular step that fails, since later steps depend on it.</p>
 * <p>The <b>Invoker</b> in the <b>Command</b> pattern (GoF): it runs every {@link ScenarioStep}
 * the same way without knowing what it does. Errors are translated by the same
 * {@link ErrorHandlerChain} as the GUI, so expected errors get the GUI's language keys.</p>
 */
public final class ScenarioRunner {

    private final ErrorHandler errorChain = ErrorHandlerChain.createDefault();

    public List<StepResult> run(List<ScenarioStep> steps) {
        Objects.requireNonNull(steps, "steps");
        ScenarioContext ctx = new ScenarioContext();
        List<StepResult> results = new ArrayList<>();

        for (ScenarioStep step : steps) {
            StepResult result;
            try {
                step.execute(ctx);
                result = StepResult.success(step);
            } catch (RuntimeException e) {
                result = StepResult.failure(step, errorChain.resolve(e));
            }
            results.add(result);

            if (!result.isAsExpected() && !step.expectsFailure()) {
                break;
            }
        }
        return results;
    }
}
